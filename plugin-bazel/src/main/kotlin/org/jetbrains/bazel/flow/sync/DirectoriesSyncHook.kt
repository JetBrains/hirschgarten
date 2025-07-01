package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.flow.open.exclude.BazelSymlinkExcludeService
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import java.net.URI
import java.nio.file.Path

private class DirectoriesSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    environment.withSubtask("Collect project directories") {
      val directories = query("workspace/directories") { environment.server.workspaceDirectories() }
      val workspaceContext =
        query("workspace/context") {
          environment.server.workspaceContext()
        }

      val additionalExcludes = BazelSymlinkExcludeService.getInstance(environment.project).getBazelSymlinksToExclude()
      val indexAllFilesInIncludedRoots = workspaceContext.indexAllFilesInDirectories.value
      val entity = createEntity(environment.project, directories, additionalExcludes, indexAllFilesInIncludedRoots)

      environment.diff.workspaceModelDiff.mutableEntityStorage
        .addEntity(entity)
    }
  }

  private fun String.toVirtualFileUrl(virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl {
    return if (this.startsWith("file://")) {
      /*
        Apparently on some operating systems(windows)
        VirtualFileUrlManager is unable to decode uri obtained from Path#toUri correctly
        on unix-like operating system uri path will look something like that:
          - file:///home/user/something - valid
        on windows path still contains root directory '/' slash
          - file:///C:/Users/user/something - valid
        VirtualFileUrlManager does not handle that and on windows returns path from root
           /C:/Users/user/something - invalid
       */
      virtualFileUrlManager.fromPath(Path.of(URI.create(this)).toAbsolutePath().toString())
    } else {
      virtualFileUrlManager.getOrCreateFromUrl(this)
    }
  }

  private suspend fun createEntity(
    project: Project,
    directories: WorkspaceDirectoriesResult,
    additionalExcludes: List<Path>,
    indexAllFilesInIncludedRoots: Boolean,
  ): BazelProjectDirectoriesEntity.Builder {
    val virtualFileUrlManager = project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

    val includedRoots = directories.includedDirectories.map { it.uri.toVirtualFileUrl(virtualFileUrlManager) }
    val excludedRoots =
      directories.excludedDirectories.map { it.uri.toVirtualFileUrl(virtualFileUrlManager) } +
        additionalExcludes.map { it.toVirtualFileUrl(virtualFileUrlManager) }

    return BazelProjectDirectoriesEntity(
      projectRoot = project.rootDir.toVirtualFileUrl(virtualFileUrlManager),
      includedRoots = includedRoots,
      excludedRoots = excludedRoots,
      indexAllFilesInIncludedRoots = indexAllFilesInIncludedRoots,
      entitySource = BazelProjectEntitySource,
    )
  }
}
