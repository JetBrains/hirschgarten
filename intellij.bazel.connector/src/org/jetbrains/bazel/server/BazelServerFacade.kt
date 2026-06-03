package org.jetbrains.bazel.server

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
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
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult

interface BazelServerFacade {
  @get:ApiStatus.Internal
  val bazelInfo: BazelInfo

  @get:ApiStatus.Internal
  val outFileHardLinks: BazelOutFileHardLinks

  @get:ApiStatus.Internal
  val bazelPathsResolver: BazelPathsResolver

  val workspaceContext: WorkspaceContext

  @ApiStatus.Internal
  suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): AspectSyncProject

  @ApiStatus.Internal
  suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): PhasedSyncProject

  // TODO: should be replaced with some query that can be crafted on the client side
  @ApiStatus.Internal
  suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult

  @ApiStatus.Internal
  suspend fun buildTargetCompile(params: CompileParams): CompileResult

  /**
   * [buildTargetRun] can also be used for debugging if you pass params into [org.jetbrains.bsp.protocol.RunParams.additionalBazelParams]
   */
  @ApiStatus.Internal
  suspend fun buildTargetRun(params: RunParams): RunResult

  @ApiStatus.Internal
  suspend fun buildTargetTest(params: TestParams): TestResult

  @ApiStatus.Internal
  suspend fun workspaceDirectories(repoMapping: RepoMapping, taskId: TaskId): WorkspaceDirectoriesResult

  @ApiStatus.Internal
  suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult

  @ApiStatus.Internal
  suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo
}
