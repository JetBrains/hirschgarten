package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseTargetToBspMapper
import org.jetbrains.bsp.protocol.BazelProject
import org.jetbrains.bsp.protocol.BspJvmClasspath
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.WorkspaceBazelPathsResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceNameResult
import org.jetbrains.bsp.protocol.WorkspacePhasedBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceTargetClasspathQueryParams

/** A facade for all project sync related methods  */
class ProjectSyncService(
  private val bspMapper: BspProjectMapper,
  private val firstPhaseTargetToBspMapper: FirstPhaseTargetToBspMapper,
  private val projectProvider: BazelSyncProjectProvider,
  private val bazelInfo: BazelInfo
) {
  suspend fun runSync(build: Boolean, taskId: TaskId): BazelProject {
    val project = projectProvider.refreshAndGet(build = build, taskId = taskId)
    return BazelProject(
      targets = project.targets,
      hasError = project.hasError,
    )
  }

  suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): WorkspaceBuildTargetsResult {
    return when (val selector = params.selector) {
      WorkspaceBuildTargetSelector.AllTargets -> {
        val project =
          projectProvider.getOrLoad(params.taskId) as? AspectSyncProject
            ?: return WorkspaceBuildTargetsResult(emptyMap(), setOf())
        bspMapper.workspaceTargets(project)
      }

      is WorkspaceBuildTargetSelector.SpecificTargets -> {
        val project =
          projectProvider.updateAndGet(
            targetsToSync = selector.targets,
            taskId = params.taskId
          )
        bspMapper.workspaceTargets(project)
      }
    }
  }

  suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): WorkspacePhasedBuildTargetsResult {
    val project = projectProvider.bazelQueryRefreshAndGet(params.taskId)
    return firstPhaseTargetToBspMapper.toWorkspaceBuildTargetsResult(project)
  }

  suspend fun workspaceDirectories(): WorkspaceDirectoriesResult {
    return bspMapper.workspaceDirectories(bazelInfo.workspaceRoot)
  }

  suspend fun workspaceBazelRepoMapping(taskId: TaskId): WorkspaceBazelRepoMappingResult {
    val project = projectProvider.getOrLoad(taskId)
    return bspMapper.workspaceBazelRepoMapping(project)
  }

  fun workspaceBazelPaths(): WorkspaceBazelPathsResult =
    WorkspaceBazelPathsResult(bazelInfo.bazelBin.toString(), bazelInfo.execRoot.toString(), BazelPathsResolver(bazelInfo))

  suspend fun workspaceName(taskId: TaskId): WorkspaceNameResult {
    val project = projectProvider.getOrLoad(taskId) as? AspectSyncProject ?: return WorkspaceNameResult(null)
    return WorkspaceNameResult(project.workspaceName)
  }

  suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult {
    val project = projectProvider.getOrLoad(inverseSourcesParams.taskId) as? AspectSyncProject ?: return InverseSourcesResult(emptyMap())
    return bspMapper.inverseSources(project, inverseSourcesParams)
  }

  suspend fun buildJvmToolchainInfoForTarget(target: Label): JvmToolchainInfo {
    return bspMapper.jvmBuilderParamsForTarget(target)
  }

  suspend fun workspaceTargetClasspathQuery(params: WorkspaceTargetClasspathQueryParams): BspJvmClasspath {
    return bspMapper.classpathQuery(params.target)
  }
}
