package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.exclude.BazelSymlinkExcludeService
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import java.nio.file.Path

private class DirectoriesSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Collect project directories") {
      val directories = query("workspace/directories") { environment.server.workspaceDirectories() }
      val workspaceContext =
        query("workspace/context") {
          environment.server.workspaceContext()
        }

      val additionalExcludes = BazelSymlinkExcludeService.getInstance(environment.project).getOrComputeBazelSymlinksToExclude()
      val indexAllFilesInIncludedRoots = workspaceContext.indexAllFilesInDirectories
      val entity = createEntity(environment.project, directories, additionalExcludes, indexAllFilesInIncludedRoots)

      environment.diff.workspaceModelDiff.mutableEntityStorage
        .addEntity(entity)
    }
  }

  private suspend fun createEntity(
    project: Project,
    directories: WorkspaceDirectoriesResult,
    additionalExcludes: Set<Path>,
    indexAllFilesInIncludedRoots: Boolean,
  ): BazelProjectDirectoriesEntityBuilder {
    val virtualFileUrlManager = project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

    val includedRoots = directories.includedDirectories.map { IdeaVFSUtil.toVirtualFileUrl(it.uri, virtualFileUrlManager) }
    val excludedRoots =
      directories.excludedDirectories.map { IdeaVFSUtil.toVirtualFileUrl(it.uri, virtualFileUrlManager) } +
        additionalExcludes.map { it.toVirtualFileUrl(virtualFileUrlManager) }

    return BazelProjectDirectoriesEntity(
      projectRoot = project.rootDir.toVirtualFileUrl(virtualFileUrlManager),
      includedRoots = includedRoots,
      excludedRoots = excludedRoots,
      indexAllFilesInIncludedRoots = indexAllFilesInIncludedRoots,
      indexAdditionalFiles = emptyList(), // Set inside IndexAdditionalFilesSyncHook
      entitySource = BazelProjectEntitySource,
    )
  }
}
