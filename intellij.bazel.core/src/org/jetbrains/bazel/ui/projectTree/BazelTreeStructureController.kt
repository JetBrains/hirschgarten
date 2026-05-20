package org.jetbrains.bazel.ui.projectTree

import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.diagnostic.telemetry.helpers.use
import com.intellij.platform.workspace.storage.CachedValue
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.performance.bspTracer
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.EXCLUDED
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.ROOT
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.UNIMPORTED
import org.jetbrains.bazel.ui.projectTree.BazelTreeStructureContext.Companion.buildBazelTreeStructureContext
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspace.excludedRoots
import org.jetbrains.bazel.workspace.includedRoots
import org.jetbrains.bazel.workspacemodel.entities.NonIndexableVirtualFileUrl

@ApiStatus.Internal
@Service(Service.Level.PROJECT)
class BazelTreeStructureController(private val project: Project) {
  fun shouldShowNode(bazelTreeNodeType: BazelTreeNodeType): Boolean =
    measure("BazelTreeStructureController.shouldShowNode.$bazelTreeNodeType") {
      val context = getOrComputeBazelTreeStructureContext()
      return@measure when (bazelTreeNodeType) {
        ROOT -> true
        EXCLUDED -> context.projectImported && nodeHasAnyDirectories(context, EXCLUDED)
        UNIMPORTED -> context.projectImported && nodeHasAnyDirectories(context, UNIMPORTED)
      }
    }

  private fun nodeHasAnyDirectories(context: BazelTreeStructureContext, bazelTreeNodeType: BazelTreeNodeType): Boolean {
    val rootProjectDir = context.rootProjectDir ?: return false
    val directories = runReadActionBlocking {
      rootProjectDir.children.filter { it.isDirectory }
    }
    return directories.any { shouldShowDirectoryUnderTreeNode(context, it, bazelTreeNodeType) }
  }

  fun shouldShowUnderTreeNode(file: VirtualFile, bazelTreeNodeType: BazelTreeNodeType): Boolean =
    measure("BazelTreeStructureController.shouldShowUnderTreeNode.$bazelTreeNodeType") {
      val context = getOrComputeBazelTreeStructureContext()
      if (!context.projectImported) {
        // When project isn't imported, we don't have information to determine the correct node,
        // so we put everything under the root node
        return@measure bazelTreeNodeType == ROOT
      }

      return@measure if (file.isDirectory) {
        shouldShowDirectoryUnderTreeNode(context, file, bazelTreeNodeType)
      }
      else {
        shouldShowFileUnderTreeNode(context, file, bazelTreeNodeType)
      }
    }

  private fun shouldShowFileUnderTreeNode(context: BazelTreeStructureContext, file: VirtualFile, bazelTreeNodeType: BazelTreeNodeType): Boolean {
    val parent = file.parent ?: return false
    if (parent == context.rootProjectDir) {
      // Files under the project root directory should show ONLY under the root node
      return bazelTreeNodeType == ROOT
    }

    if (bazelTreeNodeType != ROOT) {
      if (shouldShowDirectoryUnderTreeNode(context, parent, ROOT)) {
        // If the file is already shown under the root node, don't show it under any other node to avoid duplication
        return false
      }
    }

    return shouldShowDirectoryUnderTreeNode(context, parent, bazelTreeNodeType)
  }

  private fun shouldShowDirectoryUnderTreeNode(context: BazelTreeStructureContext, directory: VirtualFile, bazelTreeNodeType: BazelTreeNodeType): Boolean {
    if (bazelTreeNodeType == ROOT && context.isDirectoryContainingAnotherIncludedDirectory(directory)) {
      return true
    }
    if (bazelTreeNodeType == EXCLUDED && context.isDirectoryContainingAnotherExcludedDirectory(directory)) {
      return true
    }

    return computeNodeTypeForFile(directory) == bazelTreeNodeType
  }

  /**
   * Inspired by [WorkspaceFileIndexDataImpl.getFileInfo], which we can't use here directly
   * because the whole project root is registered as a workspace content,
   * so it won't give us data about unimported directories.
   */
  private fun computeNodeTypeForFile(virtualFile: VirtualFile): BazelTreeNodeType {
    val includedRoots = project.includedRoots() ?: return ROOT
    val excludedRoots = project.excludedRoots() ?: return ROOT
    var current: VirtualFile? = virtualFile
    while (current != null) {
      if (current in includedRoots) return ROOT
      if (current in excludedRoots) return EXCLUDED
      current = current.parent
    }
    return UNIMPORTED
  }

  private fun getOrComputeBazelTreeStructureContext(): BazelTreeStructureContext =
    (WorkspaceModel.getInstance(project) as WorkspaceModelInternal).entityStorage.cachedValue(treeStructureContextCache)

  private val treeStructureContextCache =
    CachedValue { it.buildBazelTreeStructureContext() }

  private inline fun <T> measure(spanName: String, crossinline f: () -> T): T =
    bspTracer.spanBuilder(spanName).use { f() }

  companion object {
    fun getInstance(project: Project): BazelTreeStructureController = project.getService(BazelTreeStructureController::class.java)
  }
}

private data class BazelTreeStructureContext(
  val projectImported: Boolean = false,
  val rootProjectDir: VirtualFile? = null,
  private val directoriesContainingIncludedDirectories: Set<VirtualFile> = emptySet(),
  private val directoriesContainingExcludedDirectories: Set<VirtualFile> = emptySet(),
) {
  fun isDirectoryContainingAnotherIncludedDirectory(file: VirtualFile): Boolean = directoriesContainingIncludedDirectories.contains(file)

  fun isDirectoryContainingAnotherExcludedDirectory(file: VirtualFile): Boolean = directoriesContainingExcludedDirectories.contains(file)

  companion object {
    fun EntityStorage.buildBazelTreeStructureContext(): BazelTreeStructureContext {
      val bazelProjectDirectories = this.bazelProjectDirectoriesEntity() ?: return BazelTreeStructureContext()
      return BazelTreeStructureContext(
        projectImported = true,
        rootProjectDir = bazelProjectDirectories.projectRoot.virtualFile,
        directoriesContainingIncludedDirectories = directoriesContainingDirectories(bazelProjectDirectories.includedRoots),
        directoriesContainingExcludedDirectories = directoriesContainingDirectories(bazelProjectDirectories.excludedRoots),
      )
    }

    private fun directoriesContainingDirectories(roots: List<NonIndexableVirtualFileUrl>): Set<VirtualFile> {
      val result =
        roots
          .asSequence()
          .mapNotNull { it.url.virtualFile }
          .mapNotNull { if (it.isDirectory) it else it.parent }
          .toMutableSet()

      val toVisit = ArrayDeque(result)
      while (toVisit.isNotEmpty()) {
        val dir = toVisit.removeFirst()
        val parent = dir.parent ?: continue
        if (parent in result) continue
        result.add(parent)
        toVisit.add(parent)
      }

      return result
    }
  }
}

@ApiStatus.Internal
enum class BazelTreeNodeType {
  ROOT,
  EXCLUDED,
  UNIMPORTED,
}
