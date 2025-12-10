package org.jetbrains.bazel.sync_new.lifecycle

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.entities
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncLifecycleListener
import org.jetbrains.bazel.sync_new.flow.SyncProgressReporter
import org.jetbrains.bazel.sync_new.flow.SyncStatus
import org.jetbrains.bazel.workspace.indexAdditionalFiles.INDEX_ADDITIONAL_FILES_DEFAULT
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.modifyBazelProjectDirectoriesEntity

// legacy ported
// TODO: make it incremental
class IndexAdditionalFilesLifecycleListener : SyncLifecycleListener {

  override suspend fun onPostSync(ctx: SyncContext, status: SyncStatus, progress: SyncProgressReporter) {
    progress.task.withTask("index_additional_files", "Indexing additional files") {
      val project = ctx.project

      val virtualFileUrlManager = project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

      val storage = WorkspaceModel.getInstance(project).currentSnapshot
      val projectDirectoriesEntity = storage.entities<BazelProjectDirectoriesEntity>().first()

      val indexAdditionalFiles: Set<VirtualFileUrl> =
        buildSet {
          this += indexAdditionalFilesByName(
            ctx = ctx,
            entityStorage = storage,
            projectDirectoriesEntity = projectDirectoriesEntity,
            virtualFileUrlManager = virtualFileUrlManager,
          )
          getProjectView(project, virtualFileUrlManager)?.let { this += it }
          this += getWorkspaceFiles(project, virtualFileUrlManager)
        }

      WorkspaceModel.getInstance(project).update("update bazel additional indexed files") { storage ->
        storage.modifyBazelProjectDirectoriesEntity(projectDirectoriesEntity) {
          this.indexAdditionalFiles += indexAdditionalFiles
        }
      }
    }
  }

  private suspend fun indexAdditionalFilesByName(
    ctx: SyncContext,
    entityStorage: EntityStorage,
    projectDirectoriesEntity: BazelProjectDirectoriesEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<VirtualFileUrl> {
    val workspaceContext = ctx.project.connection.runWithServer { server -> server.workspaceContext() }
    if (workspaceContext.indexAllFilesInDirectories) {
      return emptyList()
    }
    val indexAdditionalFilesGlob =
      ProjectViewGlobSet(workspaceContext.indexAdditionalFilesInDirectories + INDEX_ADDITIONAL_FILES_DEFAULT)

    val includedRoots = projectDirectoriesEntity.includedRoots.mapNotNull { it.virtualFile }
    val excludedRoots = projectDirectoriesEntity.excludedRoots.mapNotNull { it.virtualFile }.toSet()
    val contentRoots =
      entityStorage
        .entities<ContentRootEntity>()
        .map { it.url }
        .mapNotNull { it.virtualFile }
        .toSet()

    fun VirtualFile.isUnderContentRoot(): Boolean {
      var current: VirtualFile? = this
      while (current != null) {
        if (this in contentRoots) return true
        if (this in excludedRoots) return false
        current = current.parent
      }
      return false
    }

    val includedRootsToIterate = includedRoots.filter { !it.isUnderContentRoot() }
    val visited = mutableSetOf<VirtualFile>()

    val indexAdditionalFiles = mutableSetOf<VirtualFile>()
    val rootDir = ctx.project.rootDir

    for (includedRoot in includedRootsToIterate) {
      VfsUtilCore.visitChildrenRecursively(
        includedRoot,
        object : VirtualFileVisitor<Unit>() {
          override fun visitFileEx(file: VirtualFile): Result {
            if (file in excludedRoots || file in contentRoots) return SKIP_CHILDREN
            if (!visited.add(file)) return SKIP_CHILDREN
            if (file.isDirectory) return CONTINUE
            val relativePath = VfsUtilCore.getRelativePath(file, rootDir)
            if (relativePath != null && indexAdditionalFilesGlob.matches(relativePath)) {
              indexAdditionalFiles.add(file)
            }
            return CONTINUE
          }
        },
      )
    }

    return indexAdditionalFiles.map { it.toVirtualFileUrl(virtualFileUrlManager) }
  }

  private fun getProjectView(project: Project, virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl? {
    return project.bazelProjectSettings.projectViewPath?.toVirtualFileUrl(virtualFileUrlManager)
  }

  private fun getWorkspaceFiles(project: Project, virtualFileUrlManager: VirtualFileUrlManager): List<VirtualFileUrl> =
    Constants.WORKSPACE_FILE_NAMES
      .mapNotNull { name ->
        project.rootDir.findChild(name)
      }.map {
        it.toVirtualFileUrl(virtualFileUrlManager)
      }
}
