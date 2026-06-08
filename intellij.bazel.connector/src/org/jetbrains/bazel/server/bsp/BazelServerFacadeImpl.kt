package org.jetbrains.bazel.server.bsp

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectResolver
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult

@ApiStatus.Internal
class BazelServerFacadeImpl(
  private val bspMapper: BspProjectMapper,
  private val projectResolver: ProjectResolver,
  private val firstPhaseProjectResolver: FirstPhaseProjectResolver,
  private val executeService: ExecuteService,
  override val workspaceContext: WorkspaceContext,
  override val bazelInfo: BazelInfo,
  override val bazelPathsResolver: BazelPathsResolver,
  override val outFileHardLinks: BazelOutFileHardLinks
) : BazelServerFacade {

  override suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): AspectSyncProject {
    val targetsToSync = when (val selector = params.selector) {
      WorkspaceBuildTargetSelector.AllTargets -> null
      is WorkspaceBuildTargetSelector.SpecificTargets -> selector.targets
    }
    return projectResolver.resolve(
      build = params.build,
      targetsToSync,
      params.allTargets,
      params.taskId
    )
  }

  override suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): PhasedSyncProject {
    return firstPhaseProjectResolver.resolve(params.taskId)
  }

  override suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult {
    return bspMapper.inverseSources(bazelInfo.workspaceRoot, params)
  }

  override suspend fun buildTargetCompile(params: CompileParams): CompileResult = executeService.compile(params)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = executeService.analysisDebug(params)

  override suspend fun buildTargetTest(params: TestParams): TestResult = executeService.test(params)

  override suspend fun buildTargetRun(params: RunParams): RunResult = executeService.run(params)

  override suspend fun workspaceDirectories(repoMapping: RepoMapping, taskId: TaskId): WorkspaceDirectoriesResult {
    return bspMapper.workspaceDirectories(repoMapping, taskId)
  }

  override suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo {
    return bspMapper.jvmBuilderParamsForTarget(target)
  }
}
