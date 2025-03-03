package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
import org.jetbrains.bsp.protocol.CleanCacheParams
import org.jetbrains.bsp.protocol.CleanCacheResult
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.CppOptionsParams
import org.jetbrains.bsp.protocol.CppOptionsResult
import org.jetbrains.bsp.protocol.DependencyModulesParams
import org.jetbrains.bsp.protocol.DependencyModulesResult
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.InitializeBuildParams
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.JvmCompileClasspathParams
import org.jetbrains.bsp.protocol.JvmCompileClasspathResult
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams
import org.jetbrains.bsp.protocol.JvmRunEnvironmentResult
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams
import org.jetbrains.bsp.protocol.JvmTestEnvironmentResult
import org.jetbrains.bsp.protocol.MobileInstallParams
import org.jetbrains.bsp.protocol.MobileInstallResult
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.OutputPathsParams
import org.jetbrains.bsp.protocol.OutputPathsResult
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.RustWorkspaceParams
import org.jetbrains.bsp.protocol.RustWorkspaceResult
import org.jetbrains.bsp.protocol.ScalaMainClassesParams
import org.jetbrains.bsp.protocol.ScalaMainClassesResult
import org.jetbrains.bsp.protocol.ScalaTestClassesParams
import org.jetbrains.bsp.protocol.ScalaTestClassesResult
import org.jetbrains.bsp.protocol.ScalacOptionsParams
import org.jetbrains.bsp.protocol.ScalacOptionsResult
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
import org.jetbrains.bsp.protocol.WorkspaceBazelBinPathResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult

class BuildServerMock(
  private val initializeBuildResult: Any? = null,
  private val workspaceBuildTargetsResult: WorkspaceBuildTargetsResult? = null,
  private val sourcesResult: SourcesResult? = null,
  private val inverseSourcesResult: InverseSourcesResult? = null,
  private val dependencySourcesResult: DependencySourcesResult? = null,
  private val dependencyModulesResult: DependencyModulesResult? = null,
  private val resourcesResult: ResourcesResult? = null,
  private val outputPathsResult: OutputPathsResult? = null,
  private val compileResult: CompileResult? = null,
  private val runResult: RunResult? = null,
  private val testResult: TestResult? = null,
  private val cleanCacheResult: CleanCacheResult? = null,
  private val jvmTestEnvironmentResult: JvmTestEnvironmentResult? = null,
  private val jvmRunEnvironmentResult: JvmRunEnvironmentResult? = null,
  private val jvmCompileClasspathResult: JvmCompileClasspathResult? = null,
  private val scalacOptionsResult: ScalacOptionsResult? = null,
  private val scalaTestClassesResult: ScalaTestClassesResult? = null,
  private val scalaMainClassesResult: ScalaMainClassesResult? = null,
  private val javacOptionsResult: JavacOptionsResult? = null,
  private val cppOptionsResult: CppOptionsResult? = null,
  private val workspaceLibrariesResult: WorkspaceLibrariesResult? = null,
  private val workspaceGoLibrariesResult: WorkspaceGoLibrariesResult? = null,
  private val workspaceNonModuleTargetsResult: NonModuleTargetsResult? = null,
  private val workspaceDirectoriesResult: WorkspaceDirectoriesResult? = null,
  private val workspaceInvalidTargetsResult: WorkspaceInvalidTargetsResult? = null,
  private val analysisDebugResult: AnalysisDebugResult? = null,
  private val runResultWithDebug: RunResult? = null,
  private val mobileInstallResult: MobileInstallResult? = null,
  private val jvmBinaryJarsResult: JvmBinaryJarsResult? = null,
  private val workspaceBuildTargetsResultAndBuild: WorkspaceBuildTargetsResult? = null,
  private val workspaceBuildTargetsPartial: WorkspaceBuildTargetsResult? = null,
  private val workspaceBuildTargetsFirstPhase: WorkspaceBuildTargetsResult? = null,
  private val pythonOptionsResult: PythonOptionsResult? = null,
  private val rustWorkspaceResult: RustWorkspaceResult? = null,
  private val bazelResolveLocalToRemote: BazelResolveLocalToRemoteResult? = null,
  private val bazelResolveRemoteToLocal: BazelResolveRemoteToLocalResult? = null,
  private val workspaceBazelRepoMappingResult: WorkspaceBazelRepoMappingResult? = null,
) : JoinedBuildServer {
  override suspend fun buildInitialize(initializeBuildParams: InitializeBuildParams) {}

  override suspend fun onBuildInitialized() { // it's a mock, nothing to do
  }

  override suspend fun buildShutdown() = wrapInFuture(null)

  override suspend fun onBuildExit() { // it's a mock, nothing to do
  }

  override suspend fun workspaceBuildTargets(): WorkspaceBuildTargetsResult = wrapInFuture(workspaceBuildTargetsResult)

  override suspend fun buildTargetSources(sourcesParams: SourcesParams): SourcesResult = wrapInFuture(sourcesResult)

  override suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult =
    wrapInFuture(inverseSourcesResult)

  override suspend fun buildTargetDependencySources(dependencySourcesParams: DependencySourcesParams): DependencySourcesResult =
    wrapInFuture(dependencySourcesResult)

  override suspend fun buildTargetDependencyModules(dependencyModulesParams: DependencyModulesParams): DependencyModulesResult =
    wrapInFuture(dependencyModulesResult)

  override suspend fun buildTargetResources(resourcesParams: ResourcesParams): ResourcesResult = wrapInFuture(resourcesResult)

  override suspend fun buildTargetOutputPaths(outputPathsParams: OutputPathsParams): OutputPathsResult = wrapInFuture(outputPathsResult)

  override suspend fun buildTargetCompile(compileParams: CompileParams): CompileResult = wrapInFuture(compileResult)

  override suspend fun buildTargetRun(runParams: RunParams): RunResult = wrapInFuture(runResult)

  override suspend fun buildTargetTest(testParams: TestParams): TestResult = wrapInFuture(testResult)

  override suspend fun buildTargetCleanCache(cleanCacheParams: CleanCacheParams): CleanCacheResult = wrapInFuture(cleanCacheResult)

  override suspend fun buildTargetJvmTestEnvironment(jvmTestEnvironmentParams: JvmTestEnvironmentParams): JvmTestEnvironmentResult =
    wrapInFuture(jvmTestEnvironmentResult)

  override suspend fun buildTargetJvmRunEnvironment(jvmRunEnvironmentParams: JvmRunEnvironmentParams): JvmRunEnvironmentResult =
    wrapInFuture(jvmRunEnvironmentResult)

  override suspend fun buildTargetJvmCompileClasspath(jvmCompileClasspathParams: JvmCompileClasspathParams): JvmCompileClasspathResult =
    wrapInFuture(jvmCompileClasspathResult)

  override suspend fun buildTargetScalacOptions(scalacOptionsParams: ScalacOptionsParams): ScalacOptionsResult =
    wrapInFuture(scalacOptionsResult)

  @Deprecated("Deprecated in BSP. Use buildTarget/jvmTestEnvironment instead")
  override suspend fun buildTargetScalaTestClasses(scalaTestClassesParams: ScalaTestClassesParams): ScalaTestClassesResult =
    wrapInFuture(scalaTestClassesResult)

  @Deprecated("Deprecated in BSP. Use buildTarget/jvmRunEnvironment instead")
  override suspend fun buildTargetScalaMainClasses(scalaMainClassesParams: ScalaMainClassesParams): ScalaMainClassesResult =
    wrapInFuture(scalaMainClassesResult)

  override suspend fun buildTargetJavacOptions(javacOptionsParams: JavacOptionsParams): JavacOptionsResult =
    wrapInFuture(javacOptionsResult)

  override suspend fun buildTargetCppOptions(params: CppOptionsParams): CppOptionsResult = wrapInFuture(cppOptionsResult)

  override suspend fun workspaceLibraries(): WorkspaceLibrariesResult = wrapInFuture(workspaceLibrariesResult)

  override suspend fun workspaceGoLibraries(): WorkspaceGoLibrariesResult = wrapInFuture(workspaceGoLibrariesResult)

  override suspend fun workspaceNonModuleTargets(): NonModuleTargetsResult = wrapInFuture(workspaceNonModuleTargetsResult)

  override suspend fun workspaceDirectories(): WorkspaceDirectoriesResult = wrapInFuture(workspaceDirectoriesResult)

  override suspend fun workspaceInvalidTargets(): WorkspaceInvalidTargetsResult = wrapInFuture(workspaceInvalidTargetsResult)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = wrapInFuture(analysisDebugResult)

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = wrapInFuture(runResultWithDebug)

  override suspend fun buildTargetMobileInstall(params: MobileInstallParams): MobileInstallResult = wrapInFuture(mobileInstallResult)

  override suspend fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult = wrapInFuture(jvmBinaryJarsResult)

  override suspend fun workspaceBuildAndGetBuildTargets(): WorkspaceBuildTargetsResult = wrapInFuture(workspaceBuildTargetsResultAndBuild)

  override suspend fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): WorkspaceBuildTargetsResult =
    wrapInFuture(workspaceBuildTargetsPartial)

  override suspend fun workspaceBuildTargetsFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): WorkspaceBuildTargetsResult =
    wrapInFuture(workspaceBuildTargetsFirstPhase)

  override suspend fun buildTargetPythonOptions(params: PythonOptionsParams): PythonOptionsResult = wrapInFuture(pythonOptionsResult)

  override suspend fun rustWorkspace(params: RustWorkspaceParams): RustWorkspaceResult = wrapInFuture(rustWorkspaceResult)

  override suspend fun bazelResolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult =
    wrapInFuture(bazelResolveLocalToRemote)

  override suspend fun bazelResolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult =
    wrapInFuture(bazelResolveRemoteToLocal)

  override suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult = wrapInFuture(workspaceBazelRepoMappingResult)

  override suspend fun workspaceBazelBinPath(): WorkspaceBazelBinPathResult = WorkspaceBazelBinPathResult("/path/to/bazel-bin")

  private fun <T> wrapInFuture(value: T?): T = value ?: error("mock value is null")
}
