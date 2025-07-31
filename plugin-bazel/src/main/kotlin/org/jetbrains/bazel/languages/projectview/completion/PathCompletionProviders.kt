package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection

private fun getRelativePaths(root: VirtualFile, filter: (VirtualFile) -> Boolean): List<String> {
  val result = mutableListOf<VirtualFile>()
  VfsUtilCore.processFilesRecursively(root) { file ->
    if (filter(file)) {
      result.add(file)
    }
    true
  }

  // `VfsUtilCore.getRelativePath` can return null in the following two cases:
  // 1. The two arguments (file and ancestor) belong to different file systems.
  // 2. The second argument is not an ancestor of the first one.
  // However, since the first argument is created by traversing down the directory
  // tree starting from the second one, neither of these conditions should occur.
  return result.mapNotNull { VfsUtilCore.getRelativePath(it, root) }
}

private fun getProjectRoot(parameters: CompletionParameters): VirtualFile? = parameters.position.project.rootDir

internal class DirectoriesCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val projectRoot = getProjectRoot(parameters) ?: return
    val section = PsiTreeUtil.getParentOfType(parameters.position, ProjectViewPsiSection::class.java) ?: return
    val previousValues = section.getItems().map { it.text.trim() }
    val paths =
      getRelativePaths(projectRoot) {
        it.isDirectory && ProjectFileIndex.getInstance(parameters.position.project).isInContent(it)
      }
    paths.filter { !previousValues.contains(it) }.forEach {
      result.addElement(
        LookupElementBuilder
          .create(it)
          .withLookupStrings(listOf(it, "-$it"))
          .withIcon(PlatformIcons.FILE_ICON),
      )
    }
  }
}

internal class FiletypeCompletionProvider(val fileExtension: String) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val projectRoot = getProjectRoot(parameters) ?: return
    val section = PsiTreeUtil.getParentOfType(parameters.position, ProjectViewPsiSection::class.java) ?: return
    val previousValues = section.getItems().map { it.text.trim() }
    val paths =
      getRelativePaths(projectRoot) {
        it.name.endsWith(fileExtension) &&
          ProjectFileIndex.getInstance(parameters.position.project).isInContent(it) &&
          it.path != parameters.originalFile.virtualFile.path
      }
    paths.filter { !previousValues.contains(it) }.forEach {
      result.addElement(
        LookupElementBuilder
          .create(it)
          .withIcon(PlatformIcons.FILE_ICON),
      )
    }
  }
}

internal class ImportCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val projectRoot = getProjectRoot(parameters) ?: return
    val paths =
      getRelativePaths(projectRoot) {
        it.name.endsWith(".bazelproject") &&
          ProjectFileIndex.getInstance(parameters.position.project).isInContent(it) &&
          it.path != parameters.originalFile.virtualFile.path
      }
    result.addAllElements(paths.map { LookupElementBuilder.create(it) })
  }
}
