package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

data class BazelProject(val targets: Map<Label, BspTargetInfo.TargetInfo>, val hasError: Boolean)

interface JoinedBuildServer {
  suspend fun runSync(build: Boolean, originId: String): BazelProject

  suspend fun workspaceBuildTargets(): WorkspaceBuildTargetsResult

  suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult

  suspend fun buildTargetDependencySources(params: DependencySourcesParams): DependencySourcesResult

  suspend fun buildTargetCompile(params: CompileParams): CompileResult

  suspend fun buildTargetRun(params: RunParams): RunResult

  suspend fun buildTargetTest(params: TestParams): TestResult

  suspend fun bazelResolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult

  suspend fun bazelResolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult

  suspend fun buildTargetJvmTestEnvironment(params: JvmTestEnvironmentParams): JvmTestEnvironmentResult

  suspend fun buildTargetJvmRunEnvironment(params: JvmRunEnvironmentParams): JvmRunEnvironmentResult

  suspend fun buildTargetScalacOptions(params: ScalacOptionsParams): ScalacOptionsResult

  suspend fun buildTargetJavacOptions(params: JavacOptionsParams): JavacOptionsResult

  suspend fun buildTargetCppOptions(params: CppOptionsParams): CppOptionsResult

  suspend fun workspaceLibraries(): WorkspaceLibrariesResult

  suspend fun workspaceGoLibraries(): WorkspaceGoLibrariesResult

  suspend fun workspaceDirectories(): WorkspaceDirectoriesResult

  suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult

  suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult

  suspend fun buildTargetMobileInstall(params: MobileInstallParams): MobileInstallResult

  suspend fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult

  suspend fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): WorkspaceBuildTargetsResult

  suspend fun workspaceBuildTargetsFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): WorkspaceBuildTargetsResult

  suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult

  suspend fun workspaceBazelPaths(): WorkspaceBazelPathsResult

  suspend fun workspaceName(): WorkspaceNameResult

  suspend fun workspaceContext(): WorkspaceContext

  suspend fun jvmToolchainInfo(): JvmToolchainInfo
}
