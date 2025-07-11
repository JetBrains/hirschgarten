package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext

private fun getRelativePaths(project: Project, filter: (VirtualFile) -> Boolean): List<String> {
  val projectBasePath = project.basePath?.toNioPathOrNull() ?: return emptyList()
  val root = VirtualFileManager.getInstance().findFileByNioPath(projectBasePath) ?: return emptyList()

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
  return result.map { VfsUtilCore.getRelativePath(it, root)!! }
}

class DirectoriesCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val paths =
      getRelativePaths(parameters.position.project) {
        it.isDirectory && ProjectFileIndex.getInstance(parameters.position.project).isInContent(it)
      }
    paths.forEach {
      result.addElement(
        LookupElementBuilder
          .create(it)
          .withLookupString(it)
          .withLookupString("-$it")
          .withIcon(PlatformIcons.FILE_ICON),
      )
    }
  }
}

class ImportCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val paths =
      getRelativePaths(parameters.position.project) {
        it.name.endsWith(".bazelproject") && ProjectFileIndex.getInstance(parameters.position.project).isInContent(it)
      }
    paths.forEach {
      result.addElement(
        LookupElementBuilder
          .create(it)
          .withIcon(PlatformIcons.FILE_ICON),
      )
    }
  }
}
