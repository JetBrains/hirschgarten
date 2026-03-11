package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

interface BazelServerFacade {
  @get:ApiStatus.Internal
  val bazelInfo: BazelInfo
  val workspaceContext: WorkspaceContext

  @ApiStatus.Internal
  suspend fun runSync(build: Boolean, taskId: TaskId): WorkspaceBuildTargetsResult

  @ApiStatus.Internal
  suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): WorkspaceBuildTargetsResult

  @ApiStatus.Internal
  suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): WorkspacePhasedBuildTargetsResult

  // TODO: should be replaced with some query that can be crafted on the client side
  @ApiStatus.Internal
  suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult

  @ApiStatus.Internal
  suspend fun buildTargetCompile(params: CompileParams): CompileResult

  @ApiStatus.Internal
  suspend fun buildTargetRun(params: RunParams): RunResult

  @ApiStatus.Internal
  suspend fun buildTargetTest(params: TestParams): TestResult

  @ApiStatus.Internal
  suspend fun workspaceDirectories(): WorkspaceDirectoriesResult

  @ApiStatus.Internal
  suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult

  @ApiStatus.Internal
  suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult

  @ApiStatus.Internal
  suspend fun workspaceBazelRepoMapping(taskId: TaskId): WorkspaceBazelRepoMappingResult

  @ApiStatus.Internal
  suspend fun workspaceBazelPaths(): WorkspaceBazelPathsResult

  @ApiStatus.Internal
  suspend fun workspaceName(taskId: TaskId): WorkspaceNameResult

  @ApiStatus.Internal
  suspend fun workspaceTargetClasspathQuery(params: WorkspaceTargetClasspathQueryParams): BspJvmClasspath

  @ApiStatus.Internal
  suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo
}
