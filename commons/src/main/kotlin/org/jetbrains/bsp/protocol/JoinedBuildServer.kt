package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

data class BazelProject(val targets: Map<Label, BspTargetInfo.TargetInfo>, val hasError: Boolean)

interface JoinedBuildServer {
  suspend fun runSync(build: Boolean, originId: String): BazelProject

  suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): WorkspaceBuildTargetsResult

  suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): WorkspacePhasedBuildTargetsResult

  suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult

  suspend fun buildTargetCompile(params: CompileParams): CompileResult

  suspend fun buildTargetRun(params: RunParams): RunResult

  suspend fun buildTargetTest(params: TestParams): TestResult

  suspend fun workspaceDirectories(): WorkspaceDirectoriesResult

  suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult

  suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult

  suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult

  suspend fun workspaceBazelPaths(): WorkspaceBazelPathsResult

  suspend fun workspaceName(): WorkspaceNameResult

  suspend fun workspaceContext(): WorkspaceContext

  // TODO: implement language-specific global build metadata exchange
  suspend fun jvmToolchainInfo(): JvmToolchainInfo

  suspend fun workspaceTargetClasspathQuery(params: WorkspaceTargetClasspathQueryParams): BspJvmClasspath
}
