package org.jetbrains.bazel.languages.starlark.references

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReferenceBase
import com.intellij.util.PlatformIcons
import com.intellij.util.containers.reverse
import org.jetbrains.bazel.commons.constants.Constants.BUILD_FILE_NAMES
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkListLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.getCompletionLookupElemenent
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkFilenameLoadValue
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.languages.starlark.rename.StarlarkStringLiteralManipulator
import org.jetbrains.bazel.languages.starlark.repomapping.findContainingBazelRepo
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspace.canonicalRepoNameToPath
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import kotlin.io.path.Path
import kotlin.io.path.relativeToOrNull

// Tested in ExternalRepoResolveTest
internal class BazelLabelReference(element: StarlarkStringLiteralExpression, soft: Boolean) :
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
    if (isUseExtensionArgument()) return loadFilenameCompletion()

    return emptyArray()
  }

  override fun bindToElement(element: PsiElement): PsiElement? {
    val file = (element as? PsiFile)?.virtualFile ?: return null
    val filePath = Path(file.path)
    val repo = findContainingBazelRepo(file) ?: return null
    val repoPath = Path(repo.path)
    val packagePath = findPackagePathForFileInRepo(file, repo) ?: return null

    val packageName = packagePath.relativeToOrNull(repoPath) ?: return null
    val targetName = filePath.relativeToOrNull(packagePath) ?: return null
    val label =
      ResolvedLabel(
        repo = Canonical.createCanonicalOrMain(element.project.canonicalRepoNameToPath.reverse()[repoPath] ?: return null),
        packagePath = Package(packageName.toString().split("/")),
        target = SingleTarget(targetName.toString()),
      )

    val range = StarlarkStringLiteralManipulator().getRangeInElement(this.element)
    val newContent = label.toShortString(element.project)
    return ElementManipulators.getManipulator(this.element)?.handleContentChange(this.element, range, newContent)
  }

  override fun handleElementRename(newElementName: String): PsiElement {
    val newContent = buildNewLabelContent(element.getStringContents(), newElementName)
    val range = ElementManipulators.getValueTextRange(element)
    return ElementManipulators.getManipulator(element)?.handleContentChange(element, range, newContent) ?: element
  }

  private fun buildNewLabelContent(oldContent: String, newFilename: String): String {
    val label = Label.parseOrNull(oldContent)
    val fileNameIndex =
      if (label != null && oldContent.contains(":")) {
        oldContent.lastIndexOf(':') + 1 + label.target.toString().lastIndexOf('/')
      } else {
        oldContent.lastIndexOf('/')
      }
    return if (fileNameIndex >= 0) oldContent.take(fileNameIndex + 1) + newFilename else newFilename
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
          getCompletionLookupElemenent(
            VfsUtilCore
              .getRelativePath(it, currentDirectory)!!,
            PlatformIcons.FILE_ICON,
          )
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

  private fun VirtualFile.isBazelFile(): Boolean = BUILD_FILE_NAMES.any { name == it }

  private fun isTargetCompletionLocation(): Boolean { // TODO: Correct target completion location validation.
    val parent = element.parent ?: return false
    return parent is StarlarkListLiteralExpression
  }

  private fun targetCompletion(): Array<LookupElement> {
    val project = element.project
    return project.targetUtils
      .allTargetsAndLibrariesLabels
      .map { getCompletionLookupElemenent(it, PlatformIcons.PACKAGE_ICON) }
      .toTypedArray()
  }

  private fun isInNameArgument(): Boolean {
    val parent = element.parent ?: return false
    return parent is StarlarkNamedArgumentExpression && parent.isNameArgument()
  }

  private fun isLoadFilenameCompletionLocation(): Boolean = element.parent is StarlarkFilenameLoadValue

  private fun loadFilenameCompletion(): Array<LookupElement> {
    val repoNameToBzlFiles = getCanonicalRepoNameToBzlFiles(element.project)
    val lookupElements = mutableListOf<LookupElement>()
    for ((_, bzlFiles) in repoNameToBzlFiles) {
      for (label in bzlFiles) {
        lookupElements.add(
          getCompletionLookupElemenent(
            label.toShortString(element.project),
            PlatformIcons.FILE_ICON,
          ),
        )
      }
    }
    return lookupElements.toTypedArray()
  }

  private fun isUseExtensionArgument(): Boolean =
    (((element.parent as? StarlarkArgumentExpression)?.parent as? StarlarkArgumentList)?.parent as? StarlarkCallExpression)
      ?.getNamePsi()?.text == "use_extension"

  companion object {
  }
}
