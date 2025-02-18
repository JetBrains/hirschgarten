package org.jetbrains.bazel.sync.defaults

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.OutputPathsItem
import ch.epfl.scala.bsp4j.OutputPathsParams
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.config.bspBuildToolId
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.queryIf
import org.jetbrains.bazel.workspacemodel.entities.BspProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BspProjectEntitySource
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer

class OutputPathUrisSyncHook : ProjectSyncHook {
  override val buildToolId: BuildToolId = bspBuildToolId

  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    coroutineScope {
      val outputPaths =
        environment.server.queryOutputPaths(
          allTargetIds = environment.baseTargetInfos.allTargetIds,
          capabilities = environment.capabilities,
        )

      val entity = outputPaths.toEntity(environment.project)
      environment.diff.workspaceModelDiff.mutableEntityStorage
        .addEntity(entity)
    }
  }

  private suspend fun JoinedBuildServer.queryOutputPaths(
    allTargetIds: List<BuildTargetIdentifier>,
    capabilities: BazelBuildServerCapabilities,
  ): List<OutputPathsItem> =
    coroutineScope {
      queryIf(capabilities.outputPathsProvider == true, "buildTarget/outputPaths") {
        buildTargetOutputPaths(OutputPathsParams(allTargetIds))
      }?.items ?: emptyList()
    }

  private fun List<OutputPathsItem>.toEntity(project: Project): BspProjectDirectoriesEntity.Builder {
    val virtualFileUrlManager = WorkspaceModel.getInstance(project).getVirtualFileUrlManager()
    val projectRoot = project.rootDir.toVirtualFileUrl(virtualFileUrlManager)

    return BspProjectDirectoriesEntity(
      projectRoot = projectRoot,
      includedRoots = listOf(projectRoot),
      excludedRoots = toVirtualFileUrls(virtualFileUrlManager),
      entitySource = BspProjectEntitySource,
    )
  }

  private fun List<OutputPathsItem>.toVirtualFileUrls(virtualFileUrlManager: VirtualFileUrlManager): List<VirtualFileUrl> =
    flatMap { it.outputPaths }.map { it.uri }.mapNotNull { virtualFileUrlManager.getOrCreateFromUrl(it) }
}
