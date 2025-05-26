package org.jetbrains.bazel.languages.starlark.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.PlatformIcons
import org.jetbrains.bazel.commons.constants.Constants.BUILD_FILE_NAMES
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

// Tested in ExternalRepoResolveTest
class BazelLabelReference(element: StarlarkStringLiteralExpression, soft: Boolean) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), soft) {
  override fun resolve(): PsiElement? {
    val project = element.project
    if (!project.isBazelProject || isInNameArgument()) return null
    val label = Label.parseOrNull(element.getStringContents()) ?: return null
    val acceptOnlyFileTarget = element.getParentOfType<StarlarkLoadStatement>(strict = true) != null
    return resolveLabel(project, label, element.containingFile.originalFile.virtualFile, acceptOnlyFileTarget)
  }

  override fun getVariants(): Array<LookupElement> {
    if (!element.project.isBazelProject || isInNameArgument()) return emptyArray()

    if (isFileCompletionLocation()) return fileCompletion()
    if (isTargetCompletionLocation()) return targetCompletion()
    if (isLoadFilenameCompletionLocation()) return loadFilenameCompletion()
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
    val projectFileIndex = ProjectFileIndex.getInstance(element.project)
    searchForAllFiles(projectFileIndex, currentDirectory, allFiles)

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

  private fun searchForAllFiles(
    index: ProjectFileIndex,
    currentDirectory: VirtualFile,
    allFiles: MutableList<VirtualFile>,
  ) {
    if (index.isExcluded(currentDirectory)) return
    val children = currentDirectory.children
    for (child in children) {
      if (child.isFile && !child.isBazelFile()) {
        allFiles.add(child)
      } else if (child.isDirectory) {
        if (findBuildFile(child) == null) {
          searchForAllFiles(index, child, allFiles)
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

  private fun isLoadFilenameCompletionLocation(): Boolean = element.parent is StarlarkFilenameLoadValue

  private fun loadFilenameCompletion(): Array<LookupElement> {
    val repoNameToBzlFiles = element.project.getService(BazelFileService::class.java).getApparentRepoNameToFiles()
    val lookupElements = mutableListOf<LookupElement>()
    for ((_, bzlFiles) in repoNameToBzlFiles) {
      for (label in bzlFiles) {
        lookupElements.add(fileLookupElement(label.toShortString(element.project)))
      }
    }
    return lookupElements.toTypedArray()
  }

  companion object {
    private val LOG = logger<BazelLabelReference>()
  }
}
