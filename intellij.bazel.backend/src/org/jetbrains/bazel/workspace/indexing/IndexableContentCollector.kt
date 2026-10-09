package org.jetbrains.bazel.workspace.indexing

import com.intellij.openapi.extensions.forEachExtensionSafeInline
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.backend.workspace.storeAndGet
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.projectViewPath
import org.jetbrains.bazel.workspace.ProjectViewGlobSet

private val BAZEL_FILES_PATTERNS = with(Constants) {
  WORKSPACE_FILE_NAMES + BUILD_FILE_NAMES + MODULE_BAZEL_FILE_NAME + SUPPORTED_EXTENSIONS.map { extension -> "*.$extension" }
}

/**
 * Selects the files that should be indexed but are not under [contentRoots] already.
 * Files to index:
 * 1. All files matched by `index` patterns and the [BAZEL_FILES_PATTERNS] patterns in [includedRoots], where the inner-most root wins.
 * 2. The workspace files directly in the project root directory
 * 3. The selected project view file
 * 4. The files from [IndexableContentContributor].
 */
@ApiStatus.Internal
class IndexableContentCollector(
  private val project: Project,
  indexPatterns: List<String>,
  private val includedRoots: Set<VirtualFile>,
  private val excludedRoots: Set<VirtualFile>,
  private val contentRoots: Set<VirtualFile>,
) {

  private val projectRoot = project.rootDir
  private val workspaceFileNames = Constants.WORKSPACE_FILE_NAMES
  private val projectViewPath = project.projectViewPath()
  private val indexingGlob = ProjectViewGlobSet.of(projectRoot.toNioPath(), indexPatterns + BAZEL_FILES_PATTERNS)

  fun collect(urlManager: VirtualFileUrlManager): IndexableContent = buildIndexableContent {
    addIncludedIndexableContent(urlManager)
    addAlwaysIndexableContent(urlManager)
  }

  /**
   * Returns true if a given file should be indexed.
   */
  fun shouldBeIndexed(file: VirtualFile): Boolean =
    file.isValid &&
    !file.isDirectory &&
    (file.isRootWorkspaceFile() || file.isProjectViewFile() || file.isMatchUnderIncludedRoots())

  private fun MutableIndexableContent.addIncludedIndexableContent(urlManager: VirtualFileUrlManager) {
    val visited = mutableSetOf<VirtualFile>()
    for (includedRoot in includedRoots) {
      if (!includedRoot.isUnderIndexableIncludedRoot()) continue
      VfsUtilCore.visitChildrenRecursively(
        includedRoot,
        object : VirtualFileVisitor<Unit>() {
          override fun visitFileEx(file: VirtualFile): Result = when {
            file in excludedRoots || file in contentRoots || !visited.add(file) -> SKIP_CHILDREN
            file.isDirectory && file.impliesRecursiveMatch() -> {
              recursiveRoots.add(urlManager.storeAndGet(file))
              SKIP_CHILDREN
            }
            !file.isDirectory && file.matchesIndexingGlob() -> {
              nonRecursiveRoots.add(urlManager.storeAndGet(file))
              CONTINUE
            }
            else -> CONTINUE
          }
        },
      )
    }
  }

  private fun MutableIndexableContent.addAlwaysIndexableContent(urlManager: VirtualFileUrlManager) {
    workspaceFileNames
      .mapNotNull(projectRoot::findChild)
      .mapTo(nonRecursiveRoots, urlManager::storeAndGet)
    projectViewPath
      ?.toVirtualFileUrl(urlManager)
      ?.let(nonRecursiveRoots::add)
    IndexableContentContributor.ep.forEachExtensionSafeInline { contributor ->
      this += contributor.getIndexableContent(project)
    }
  }

  private fun VirtualFile.isRootWorkspaceFile() = parent == projectRoot && name in workspaceFileNames

  private fun VirtualFile.isProjectViewFile() = projectViewPath != null && toNioPathOrNull() == projectViewPath

  private fun VirtualFile.isMatchUnderIncludedRoots() = isUnderIndexableIncludedRoot() && matchesIndexingGlob()

  /**
   * Returns true if the inner-most root above the file is an included root, and no content root contains that included root.
   * An excluded root above the included root does not change the result.
   * The file itself counts as a root, so this also selects the included roots that [addIncludedIndexableContent] walks.
   */
  private fun VirtualFile.isUnderIndexableIncludedRoot(): Boolean {
    var isUnderIncludedRoot = false
    var current: VirtualFile? = this
    while (current != null) {
      when (current) {
        in contentRoots -> return false
        in excludedRoots -> return isUnderIncludedRoot
        in includedRoots -> isUnderIncludedRoot = true
      }
      current = current.parent
    }
    return isUnderIncludedRoot
  }

  private fun VirtualFile.matchesIndexingGlob() = toNioPathOrNull()?.let(indexingGlob::matches) == true

  private fun VirtualFile.impliesRecursiveMatch() = toNioPathOrNull()?.let(indexingGlob::impliesRecursiveMatch) == true
}
