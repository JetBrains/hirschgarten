package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.sync.SyncCache
import org.jetbrains.bazel.workspace.excludedRoots

private fun Project.getRelativePaths(filter: (VirtualFile) -> Boolean): List<String> {
  val result = mutableListOf<VirtualFile>()
  val excludedRoots = excludedRoots() ?: return emptyList()
  val rootDir = rootDir
  VfsUtilCore.visitChildrenRecursively(
    rootDir,
    object : VirtualFileVisitor<Unit>() {
      override fun visitFileEx(file: VirtualFile): Result {
        if (file in excludedRoots) return SKIP_CHILDREN
        if (filter(file)) result.add(file)
        return CONTINUE
      }
    },
  )
  return result.mapNotNull { VfsUtilCore.getRelativePath(it, rootDir) }
}

class DirectoriesCompletionProvider : CompletionProvider<CompletionParameters>() {
  private val pathsComputable =
    SyncCache.SyncCacheComputable { project ->
      project.getRelativePaths { it.isDirectory }
    }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val project = parameters.position.project
    if (!project.isBazelProject) return
    val section = PsiTreeUtil.getParentOfType(parameters.position, ProjectViewPsiSection::class.java) ?: return
    val previousValues = section.getItems().map { it.text.trim() }.toSet()
    val paths = SyncCache.getInstance(project).get(pathsComputable)
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

class FiletypeCompletionProvider(val fileExtension: String) : CompletionProvider<CompletionParameters>() {
  private val pathsComputable =
    SyncCache.SyncCacheComputable { project ->
      project.getRelativePaths { it.name.endsWith(fileExtension) }
    }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val project = parameters.position.project
    if (!project.isBazelProject) return
    val section = PsiTreeUtil.getParentOfType(parameters.position, ProjectViewPsiSection::class.java) ?: return
    val previousValues = section.getItems().map { it.text.trim() }.toSet()
    val paths = SyncCache.getInstance(project).get(pathsComputable)
    val originalFileRelativePath = getOriginalFileRelativePath(parameters)
    paths.filter { !previousValues.contains(it) && it != originalFileRelativePath }.forEach {
      result.addElement(
        LookupElementBuilder
          .create(it)
          .withIcon(PlatformIcons.FILE_ICON),
      )
    }
  }
}

internal class ImportCompletionProvider : CompletionProvider<CompletionParameters>() {
  private val pathsComputable =
    SyncCache.SyncCacheComputable { project ->
      project.getRelativePaths { it.name.endsWith(".bazelproject") }
    }

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val project = parameters.position.project
    if (!project.isBazelProject) return
    val originalFileRelativePath = getOriginalFileRelativePath(parameters)
    val paths = SyncCache.getInstance(project).get(pathsComputable).filter { it != originalFileRelativePath }

    result.addAllElements(paths.map { LookupElementBuilder.create(it) })
  }
}

private fun getOriginalFileRelativePath(parameters: CompletionParameters): String? =
  VfsUtilCore.getRelativePath(parameters.originalFile.virtualFile, parameters.position.project.rootDir)
