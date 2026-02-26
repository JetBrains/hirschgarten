package org.jetbrains.bazel.server.bsp

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.sync.BazelSyncProjectProvider
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BazelProject
import org.jetbrains.bsp.protocol.BspJvmClasspath
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.BazelServerFacade
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.RawPhasedTarget
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
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

class BaselServerFacadeImpl(
  private val bspMapper: BspProjectMapper,
  private val projectProvider: BazelSyncProjectProvider,
  private val executeService: ExecuteService,
  override val workspaceContext: WorkspaceContext,
  override val bazelInfo: BazelInfo,
) : BazelServerFacade {

  override suspend fun runSync(build: Boolean, taskId: TaskId): BazelProject {
    val project = projectProvider.refreshAndGet(build = build, taskId = taskId)
    return BazelProject(
      targets = project.targets,
      hasError = project.hasError,
    )
  }

  override suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): WorkspaceBuildTargetsResult {
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
            taskId = params.taskId,
          )
        bspMapper.workspaceTargets(project)
      }
    }
  }

  override suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): WorkspacePhasedBuildTargetsResult {
    val project = projectProvider.bazelQueryRefreshAndGet(params.taskId)
    val targets = project.modules.mapValues {
      RawPhasedTarget(it.value)
    }
    return WorkspacePhasedBuildTargetsResult(targets)
  }

  override suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult {
    val project = projectProvider.getOrLoad(params.taskId) as? AspectSyncProject
                  ?: return InverseSourcesResult(emptyMap())
    return bspMapper.inverseSources(project, params)
  }

  override suspend fun buildTargetCompile(params: CompileParams): CompileResult = executeService.compile(params)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = executeService.analysisDebug(params)

  override suspend fun buildTargetTest(params: TestParams): TestResult = executeService.testWithDebug(params)

  override suspend fun buildTargetRun(params: RunParams): RunResult = executeService.run(params)

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = executeService.runWithDebug(params)

  override suspend fun workspaceDirectories(): WorkspaceDirectoriesResult {
    return bspMapper.workspaceDirectories(bazelInfo.workspaceRoot)
  }

  override suspend fun workspaceBazelRepoMapping(taskId: TaskId): WorkspaceBazelRepoMappingResult {
    val project = projectProvider.getOrLoad(taskId)
    return bspMapper.workspaceBazelRepoMapping(project)
  }

  override suspend fun workspaceBazelPaths(): WorkspaceBazelPathsResult {
    return WorkspaceBazelPathsResult(
      bazelBin = bazelInfo.bazelBin.toString(),
      executionRoot = bazelInfo.execRoot.toString(),
      bazelPathsResolver = BazelPathsResolver(bazelInfo),
    )
  }

  override suspend fun workspaceName(taskId: TaskId): WorkspaceNameResult {
    val project = projectProvider.getOrLoad(taskId) as? AspectSyncProject
                  ?: return WorkspaceNameResult(null)
    return WorkspaceNameResult(project.workspaceName)
  }

  override suspend fun workspaceTargetClasspathQuery(params: WorkspaceTargetClasspathQueryParams): BspJvmClasspath {
    return bspMapper.classpathQuery(params.target)
  }

  override suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo {
    return bspMapper.jvmBuilderParamsForTarget(target)
  }
}
