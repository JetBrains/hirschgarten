package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BazelProject
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.CppOptionsParams
import org.jetbrains.bsp.protocol.CppOptionsResult
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.MobileInstallParams
import org.jetbrains.bsp.protocol.MobileInstallResult
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
import org.jetbrains.bsp.protocol.WorkspaceBazelPathsResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceNameResult

class BuildServerMock(
  private val bazelProject: BazelProject? = null,
  private val workspaceBuildTargetsResult: WorkspaceBuildTargetsResult? = null,
  private val inverseSourcesResult: InverseSourcesResult? = null,
  private val dependencySourcesResult: DependencySourcesResult? = null,
  private val compileResult: CompileResult? = null,
  private val runResult: RunResult? = null,
  private val testResult: TestResult? = null,
  private val jvmTestEnvironmentResult: JvmTestEnvironmentResult? = null,
  private val jvmRunEnvironmentResult: JvmRunEnvironmentResult? = null,
  private val javacOptionsResult: JavacOptionsResult? = null,
  private val cppOptionsResult: CppOptionsResult? = null,
  private val workspaceLibrariesResult: WorkspaceLibrariesResult? = null,
  private val workspaceGoLibrariesResult: WorkspaceGoLibrariesResult? = null,
  private val workspaceDirectoriesResult: WorkspaceDirectoriesResult? = null,
  private val analysisDebugResult: AnalysisDebugResult? = null,
  private val runResultWithDebug: RunResult? = null,
  private val mobileInstallResult: MobileInstallResult? = null,
  private val jvmBinaryJarsResult: JvmBinaryJarsResult? = null,
  private val workspaceBuildTargetsPartial: WorkspaceBuildTargetsResult? = null,
  private val workspaceBuildTargetsFirstPhase: WorkspaceBuildTargetsResult? = null,
  private val bazelResolveLocalToRemote: BazelResolveLocalToRemoteResult? = null,
  private val bazelResolveRemoteToLocal: BazelResolveRemoteToLocalResult? = null,
  private val workspaceBazelRepoMappingResult: WorkspaceBazelRepoMappingResult? = null,
  private val workspaceContextResult: WorkspaceContext? = null,
) : JoinedBuildServer {
  override suspend fun runSync(build: Boolean, originId: String): BazelProject = wrapInFuture(bazelProject)

  override suspend fun workspaceBuildTargets(): WorkspaceBuildTargetsResult = wrapInFuture(workspaceBuildTargetsResult)

  override suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult =
    wrapInFuture(inverseSourcesResult)

  override suspend fun buildTargetDependencySources(dependencySourcesParams: DependencySourcesParams): DependencySourcesResult =
    wrapInFuture(dependencySourcesResult)

  override suspend fun buildTargetCompile(compileParams: CompileParams): CompileResult = wrapInFuture(compileResult)

  override suspend fun buildTargetRun(runParams: RunParams): RunResult = wrapInFuture(runResult)

  override suspend fun buildTargetTest(testParams: TestParams): TestResult = wrapInFuture(testResult)

  override suspend fun buildTargetJvmTestEnvironment(jvmTestEnvironmentParams: JvmTestEnvironmentParams): JvmTestEnvironmentResult =
    wrapInFuture(jvmTestEnvironmentResult)

  override suspend fun buildTargetJvmRunEnvironment(jvmRunEnvironmentParams: JvmRunEnvironmentParams): JvmRunEnvironmentResult =
    wrapInFuture(jvmRunEnvironmentResult)

  override suspend fun buildTargetJavacOptions(javacOptionsParams: JavacOptionsParams): JavacOptionsResult =
    wrapInFuture(javacOptionsResult)

  override suspend fun buildTargetCppOptions(params: CppOptionsParams): CppOptionsResult = wrapInFuture(cppOptionsResult)

  override suspend fun workspaceLibraries(): WorkspaceLibrariesResult = wrapInFuture(workspaceLibrariesResult)

  override suspend fun workspaceGoLibraries(): WorkspaceGoLibrariesResult = wrapInFuture(workspaceGoLibrariesResult)

  override suspend fun workspaceDirectories(): WorkspaceDirectoriesResult = wrapInFuture(workspaceDirectoriesResult)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = wrapInFuture(analysisDebugResult)

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = wrapInFuture(runResultWithDebug)

  override suspend fun buildTargetMobileInstall(params: MobileInstallParams): MobileInstallResult = wrapInFuture(mobileInstallResult)

  override suspend fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult = wrapInFuture(jvmBinaryJarsResult)

  override suspend fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): WorkspaceBuildTargetsResult =
    wrapInFuture(workspaceBuildTargetsPartial)

  override suspend fun workspaceBuildTargetsFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): WorkspaceBuildTargetsResult =
    wrapInFuture(workspaceBuildTargetsFirstPhase)

  override suspend fun bazelResolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult =
    wrapInFuture(bazelResolveLocalToRemote)

  override suspend fun bazelResolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult =
    wrapInFuture(bazelResolveRemoteToLocal)

  override suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult = wrapInFuture(workspaceBazelRepoMappingResult)

  override suspend fun workspaceBazelPaths(): WorkspaceBazelPathsResult =
    WorkspaceBazelPathsResult("/path/to/bazel-bin", "/path/to/bazel-out/exec")

  override suspend fun workspaceName(): WorkspaceNameResult = WorkspaceNameResult("_main")

  override suspend fun workspaceContext(): WorkspaceContext = wrapInFuture(workspaceContextResult)

  override suspend fun jvmToolchainInfo() = JvmToolchainInfo("/path/to/java/home", "/path/to/bazel/toolchain", emptyList())

  override suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo =
    JvmToolchainInfo("/path/to/java/home", "/path/to/bazel/toolchain", emptyList())

  private fun <T> wrapInFuture(value: T?): T = value ?: error("mock value is null")
}
