package org.jetbrains.bazel.sync_new.lifecycle

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.exclude.BazelSymlinkExcludeService
import org.jetbrains.bazel.flow.sync.IdeaVFSUtil
import org.jetbrains.bazel.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncLifecycleListener
import org.jetbrains.bazel.sync_new.flow.SyncProgressReporter
import org.jetbrains.bazel.sync_new.flow.SyncStatus
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.NewSyncBazelEntitySource
import java.nio.file.Path

// legacy ported
class DirectoriesLifecycleListener : SyncLifecycleListener {
  private val entitySource = NewSyncBazelEntitySource("directories")

  override suspend fun onPostSync(ctx: SyncContext, status: SyncStatus, progress: SyncProgressReporter) {
    val hasDirectoriesChanged = (ctx.session.universeChanges?.hasDirectoriesChanged ?: false)
      || ctx.scope.isFullSync
    if (!hasDirectoriesChanged) {
      return
    }
    progress.task.withTask("collect_project_dirs", "Collect project directories") {
      val workspaceContext = ctx.project.connection.runWithServer { server -> server.workspaceContext() }
      val pathsResolver = ctx.project.connection.runWithServer { server -> server.workspaceBazelPaths().bazelPathsResolver }

      val additionalDirectoriesToExclude = listOf(
        pathsResolver.dotBazelBsp(),
        pathsResolver.workspaceRoot().resolve(JPS_COMPILED_BASE_DIRECTORY),
      )
      val includedDirectories = workspaceContext.directories.filter { it.isIncluded() }.map { it.value }
      val excludedDirectories = workspaceContext.directories.filter { it.isExcluded() }.map { it.value } + additionalDirectoriesToExclude

      val additionalExcludes = BazelSymlinkExcludeService.getInstance(ctx.project).getBazelSymlinksToExclude()
      val indexAllFilesInIncludedRoots = workspaceContext.indexAllFilesInDirectories

      val entity = createEntity(
        project = ctx.project,
        includedDirs = includedDirectories,
        excludedDirs = excludedDirectories,
        additionalExcludes = additionalExcludes,
        indexAllFilesInIncludedRoots = indexAllFilesInIncludedRoots,
      )
      val newSnapshot = MutableEntityStorage.create()
      newSnapshot.addEntity(entity)

      WorkspaceModel.getInstance(ctx.project).update("update bazel directories entity") { storage ->
        storage.replaceBySource(
          sourceFilter = { it == entitySource },
          replaceWith = newSnapshot,
        )
      }
    }

  }

  private suspend fun createEntity(
    project: Project,
    includedDirs: List<Path>,
    excludedDirs: List<Path>,
    additionalExcludes: List<Path>,
    indexAllFilesInIncludedRoots: Boolean,
  ): BazelProjectDirectoriesEntityBuilder {
    val virtualFileUrlManager = project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

    val includedRoots = includedDirs.map { IdeaVFSUtil.toVirtualFileUrl(it.toUri().toString(), virtualFileUrlManager) }
    val excludedRoots =
      excludedDirs.map { IdeaVFSUtil.toVirtualFileUrl(it.toUri().toString(), virtualFileUrlManager) } +
        additionalExcludes.map { it.toVirtualFileUrl(virtualFileUrlManager) }

    return BazelProjectDirectoriesEntity(
      projectRoot = project.rootDir.toVirtualFileUrl(virtualFileUrlManager),
      includedRoots = includedRoots,
      excludedRoots = excludedRoots,
      indexAllFilesInIncludedRoots = indexAllFilesInIncludedRoots,
      indexAdditionalFiles = emptyList(), // Set inside IndexAdditionalFilesSyncHook
      entitySource = entitySource,
    )
  }

}
