package org.jetbrains.bazel.languages.starlark.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.repomapping.apparentRepoNameToCanonicalName
import org.jetbrains.bazel.languages.starlark.repomapping.canonicalRepoNameToPath
import org.jetbrains.bazel.languages.starlark.repomapping.findContainingBazelRepo
import org.jetbrains.bazel.target.targetUtils

public val BUILD_FILE_NAMES = sequenceOf("BUILD.bazel", "BUILD")

// Tested in ExternalRepoResolveTest
class BazelLabelReference(element: StarlarkStringLiteralExpression, soft: Boolean) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), soft) {
  override fun resolve(): PsiElement? {
    val project = element.project
    if (!project.isBazelProject || isInNameArgument()) return null
    val label = Label.parseOrNull(element.getStringContents()) ?: return null

    return resolveLabel(project, label, element.containingFile.originalFile.virtualFile, true)
  }

  override fun getVariants(): Array<LookupElement> {
    if (!element.project.isBazelProject || isInNameArgument()) return emptyArray()

    if (isFileCompletionLocation()) return fileCompletion()
    if (isTargetCompletionLocation()) return targetCompletion()
    return emptyArray()
  }

  // Checks whether it is the value of "src", "srcs" or "hdrs".
  private fun isFileCompletionLocation(): Boolean {
    if (element.parent is StarlarkListLiteralExpression) {
      val parentName = (element.parent.parent as? StarlarkNamedArgumentExpression)?.name
      return parentName in listOf("srcs", "hdrs")
    }
    return ((element.parent as? StarlarkNamedArgumentExpression)?.name == "src")
  }

  // Returns an array of all files (excluding BUILD ones) from the current directory and its
  // subdirectories, excluding subpackages — that is, subdirectories that contain a BUILD file.
  private fun fileCompletion(): Array<LookupElement> {
    val currentDirectory = element.containingFile.originalFile.virtualFile.parent
    val allFiles = mutableListOf<VirtualFile>()
    searchForAllFiles(currentDirectory, allFiles)

    // `VfsUtilCore.getRelativePath` can return null in the following two cases:
    // 1. The two arguments (file and ancestor) belong to different file systems.
    // 2. The second argument is not an ancestor of the first one.
    // However, since the first argument is created by traversing down the directory
    // tree starting from the second one, neither of these conditions should occur.
    val lookupElements =
      allFiles
        .map {
          fileLookupElement(VfsUtilCore.getRelativePath(it, currentDirectory)!!)
        }.toTypedArray()
    return lookupElements
  }

  private fun searchForAllFiles(currentDirectory: VirtualFile, allFiles: MutableList<VirtualFile>) {
    val children = currentDirectory.children
    for (child in children) {
      if (child.isFile && !child.isBazelFile()) {
        allFiles.add(child)
      } else if (child.isDirectory) {
        if (findBuildFile(child) == null) {
          searchForAllFiles(child, allFiles)
        }
      }
    }
  }

  private fun fileLookupElement(name: String): LookupElement =
    LookupElementBuilder
      .create("\"" + name + "\"")
      .withIcon(PlatformIcons.FILE_ICON)
      .withPresentableText(name)

  private fun VirtualFile.isBazelFile(): Boolean = BUILD_FILE_NAMES.any { name == it }

  private fun isTargetCompletionLocation(): Boolean { // TODO: Correct target completion location validation.
    val parent = element.parent ?: return false
    return parent is StarlarkListLiteralExpression
  }

  private fun targetCompletion(): Array<LookupElement> {
    val project = element.project
    val targetUtils = project.targetUtils
    return targetUtils
      .allTargetsAndLibrariesLabels
      .map { targetLookupElement(it) }
      .toTypedArray()
  }

  private fun targetLookupElement(name: String): LookupElement =
    LookupElementBuilder
      .create("\"" + name + "\"")
      .withIcon(PlatformIcons.PACKAGE_ICON)
      .withPresentableText(name)

  private fun isInNameArgument(): Boolean {
    val parent = element.parent ?: return false
    return parent is StarlarkNamedArgumentExpression && parent.isNameArgument()
  }

  private sealed interface BuildFileOrSourceFile

  private class BuildFilePsi(val file: StarlarkFile) : BuildFileOrSourceFile

  private class SourceFile(val file: VirtualFile) : BuildFileOrSourceFile

  companion object {
    fun resolveLabel(
      project: Project,
      label: Label,
      containingFile: VirtualFile?,
      allowFallbackToRootDir: Boolean = true,
    ): PsiElement? {
      val buildOrSource = resolveBuildFileOrSourceFile(project, label, containingFile, allowFallbackToRootDir) ?: return null
      return when (buildOrSource) {
        is BuildFilePsi -> resolveRuleTarget(buildOrSource.file, label) ?: resolveFileTarget(project, buildOrSource.file, label)
        is SourceFile -> PsiManager.getInstance(project).findFile(buildOrSource.file)
      }
    }

    private fun resolveBuildFileOrSourceFile(
      project: Project,
      label: Label,
      containingFile: VirtualFile?,
      allowFallbackToRootDir: Boolean = true,
    ): BuildFileOrSourceFile? =
      when (label) {
        is ResolvedLabel ->
          resolveAbsolutePackage(
            project,
            label,
            containingFile,
            allowFallbackToRootDir,
          )?.let { BuildFilePsi(it) }

        else -> resolveRelativePackageOrSourceFile(project, label, containingFile)
      }

    private fun resolveAbsolutePackage(
      project: Project,
      label: ResolvedLabel,
      containingFile: VirtualFile?,
      allowFallbackToRootDir: Boolean,
    ): StarlarkFile? {
      val packageDir = findReferredAbsolutePackage(project, containingFile, label, allowFallbackToRootDir) ?: return null
      val buildFile = findBuildFile(packageDir) ?: return null
      return findBuildFilePsi(project, buildFile)
    }

    private fun resolveRelativePackageOrSourceFile(
      project: Project,
      label: Label,
      containingFile: VirtualFile?,
    ): BuildFileOrSourceFile? {
      val containingPackage = findContainingPackage(containingFile) ?: return null
      val referredPackage = containingPackage.findFileByRelativePath(label.packagePath.toString()) ?: return null
      if (referredPackage.isFile) {
        if (label.target is AmbiguousEmptyTarget) return SourceFile(referredPackage)
        return null
      }
      val buildFile = findBuildFile(referredPackage) ?: return null
      return findBuildFilePsi(project, buildFile)?.let { BuildFilePsi(it) }
    }

    private fun findContainingPackage(directory: VirtualFile?): VirtualFile? =
      directory?.let {
        if (findBuildFile(directory) != null) directory else findContainingPackage(directory.parent)
      }

    private fun resolveRuleTarget(buildFilePsi: StarlarkFile, label: Label): StarlarkCallExpression? =
      buildFilePsi.findRuleTarget(label.targetName)

    private fun resolveFileTarget(
      project: Project,
      buildFilePsi: StarlarkFile,
      label: Label,
    ): PsiFile? {
      // buildFilePsi may be excluded, in which case we can't get the parent PsiDirectory.
      // Therefore get virtualFile then parent and not vice-versa
      val targetFile = buildFilePsi.virtualFile?.parent?.findFileByRelativePath(label.targetName) ?: return null

      return PsiManager.getInstance(project).findFile(targetFile)
    }

    private fun findReferredAbsolutePackage(
      project: Project,
      containingFile: VirtualFile?,
      label: ResolvedLabel,
      allowFallbackToRootDir: Boolean,
    ): VirtualFile? {
      val foundRepoRoot =
        if (label.repoName.isEmpty() && containingFile != null) {
          findContainingBazelRepo(project, containingFile.toNioPath())
        } else if (label.isApparent) {
          project.apparentRepoNameToCanonicalName[label.repoName]?.let { canonicalRepoName ->
            project.canonicalRepoNameToPath[canonicalRepoName]
          }
        } else {
          project.canonicalRepoNameToPath[label.repoName]
        }

      val repoRoot =
        foundRepoRoot?.let {
          VirtualFileManager.getInstance().refreshAndFindFileByNioPath(foundRepoRoot)
        } ?: if (allowFallbackToRootDir) {
          project.rootDir
        } else {
          return null
        }

      return repoRoot.findFileByRelativePath(label.packagePath.toString())
    }

    private fun findBuildFile(packageDir: VirtualFile): VirtualFile? =
      BUILD_FILE_NAMES.mapNotNull { packageDir.findChild(it) }.firstOrNull()

    private fun findBuildFilePsi(project: Project, buildFile: VirtualFile): StarlarkFile? =
      PsiManager.getInstance(project).findFile(buildFile) as? StarlarkFile
  }
}
