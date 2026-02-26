package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

data class BazelProject(val targets: Map<Label, BspTargetInfo.TargetInfo>, val hasError: Boolean)

interface BazelServerFacade {
  val bazelInfo: BazelInfo
  val workspaceContext: WorkspaceContext

  suspend fun runSync(build: Boolean, taskId: TaskId): BazelProject

  suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): WorkspaceBuildTargetsResult

  suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): WorkspacePhasedBuildTargetsResult

  // TODO: should be replaced with some query that can be crafted on the client side
  suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult

  suspend fun buildTargetCompile(params: CompileParams): CompileResult

  suspend fun buildTargetRun(params: RunParams): RunResult

  suspend fun buildTargetTest(params: TestParams): TestResult

  suspend fun workspaceDirectories(): WorkspaceDirectoriesResult

  suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult

  suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult

  suspend fun workspaceBazelRepoMapping(taskId: TaskId): WorkspaceBazelRepoMappingResult

  suspend fun workspaceBazelPaths(): WorkspaceBazelPathsResult

  suspend fun workspaceName(taskId: TaskId): WorkspaceNameResult

  suspend fun workspaceTargetClasspathQuery(params: WorkspaceTargetClasspathQueryParams): BspJvmClasspath

  suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo
}
