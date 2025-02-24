package org.jetbrains.workspace.model.test.framework

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
import org.jetbrains.bsp.protocol.InitializeBuildResult
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
import org.jetbrains.bsp.protocol.ReadParams
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
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import java.util.concurrent.CompletableFuture

class BuildServerMock(
  private val initializeBuildResult: InitializeBuildResult? = null,
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
  override fun buildInitialize(initializeBuildParams: InitializeBuildParams): CompletableFuture<InitializeBuildResult> =
    wrapInFuture(initializeBuildResult)

  override fun onBuildInitialized() { // it's a mock, nothing to do
  }

  override fun buildShutdown(): CompletableFuture<Any> = wrapInFuture(null)

  override fun onBuildExit() { // it's a mock, nothing to do
  }

  override fun workspaceBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> = wrapInFuture(workspaceBuildTargetsResult)

  override fun workspaceReload(): CompletableFuture<Any> = wrapInFuture(null)

  override fun buildTargetSources(sourcesParams: SourcesParams): CompletableFuture<SourcesResult> = wrapInFuture(sourcesResult)

  override fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): CompletableFuture<InverseSourcesResult> =
    wrapInFuture(inverseSourcesResult)

  override fun buildTargetDependencySources(dependencySourcesParams: DependencySourcesParams): CompletableFuture<DependencySourcesResult> =
    wrapInFuture(dependencySourcesResult)

  override fun buildTargetDependencyModules(dependencyModulesParams: DependencyModulesParams): CompletableFuture<DependencyModulesResult> =
    wrapInFuture(dependencyModulesResult)

  override fun buildTargetResources(resourcesParams: ResourcesParams): CompletableFuture<ResourcesResult> = wrapInFuture(resourcesResult)

  override fun buildTargetOutputPaths(outputPathsParams: OutputPathsParams): CompletableFuture<OutputPathsResult> =
    wrapInFuture(outputPathsResult)

  override fun buildTargetCompile(compileParams: CompileParams): CompletableFuture<CompileResult> = wrapInFuture(compileResult)

  override fun buildTargetRun(runParams: RunParams): CompletableFuture<RunResult> = wrapInFuture(runResult)

  override fun buildTargetTest(testParams: TestParams): CompletableFuture<TestResult> = wrapInFuture(testResult)

  override fun buildTargetCleanCache(cleanCacheParams: CleanCacheParams): CompletableFuture<CleanCacheResult> =
    wrapInFuture(cleanCacheResult)

  override fun onRunReadStdin(params: ReadParams) { // it's a mock, nothing to do
  }

  override fun buildTargetJvmTestEnvironment(
    jvmTestEnvironmentParams: JvmTestEnvironmentParams,
  ): CompletableFuture<JvmTestEnvironmentResult> = wrapInFuture(jvmTestEnvironmentResult)

  override fun buildTargetJvmRunEnvironment(jvmRunEnvironmentParams: JvmRunEnvironmentParams): CompletableFuture<JvmRunEnvironmentResult> =
    wrapInFuture(jvmRunEnvironmentResult)

  override fun buildTargetJvmCompileClasspath(
    jvmCompileClasspathParams: JvmCompileClasspathParams,
  ): CompletableFuture<JvmCompileClasspathResult> = wrapInFuture(jvmCompileClasspathResult)

  override fun buildTargetScalacOptions(scalacOptionsParams: ScalacOptionsParams): CompletableFuture<ScalacOptionsResult> =
    wrapInFuture(scalacOptionsResult)

  @Deprecated("Deprecated in BSP. Use buildTarget/jvmTestEnvironment instead")
  override fun buildTargetScalaTestClasses(scalaTestClassesParams: ScalaTestClassesParams): CompletableFuture<ScalaTestClassesResult> =
    wrapInFuture(scalaTestClassesResult)

  @Deprecated("Deprecated in BSP. Use buildTarget/jvmRunEnvironment instead")
  override fun buildTargetScalaMainClasses(scalaMainClassesParams: ScalaMainClassesParams): CompletableFuture<ScalaMainClassesResult> =
    wrapInFuture(scalaMainClassesResult)

  override fun buildTargetJavacOptions(javacOptionsParams: JavacOptionsParams): CompletableFuture<JavacOptionsResult> =
    wrapInFuture(javacOptionsResult)

  override fun buildTargetCppOptions(params: CppOptionsParams): CompletableFuture<CppOptionsResult> = wrapInFuture(cppOptionsResult)

  override fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult> = wrapInFuture(workspaceLibrariesResult)

  override fun workspaceGoLibraries(): CompletableFuture<WorkspaceGoLibrariesResult> = wrapInFuture(workspaceGoLibrariesResult)

  override fun workspaceNonModuleTargets(): CompletableFuture<NonModuleTargetsResult> = wrapInFuture(workspaceNonModuleTargetsResult)

  override fun workspaceDirectories(): CompletableFuture<WorkspaceDirectoriesResult> = wrapInFuture(workspaceDirectoriesResult)

  override fun workspaceInvalidTargets(): CompletableFuture<WorkspaceInvalidTargetsResult> = wrapInFuture(workspaceInvalidTargetsResult)

  override fun buildTargetAnalysisDebug(params: AnalysisDebugParams): CompletableFuture<AnalysisDebugResult> =
    wrapInFuture(analysisDebugResult)

  override fun buildTargetRunWithDebug(params: RunWithDebugParams): CompletableFuture<RunResult> = wrapInFuture(runResultWithDebug)

  override fun buildTargetMobileInstall(params: MobileInstallParams): CompletableFuture<MobileInstallResult> =
    wrapInFuture(mobileInstallResult)

  override fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): CompletableFuture<JvmBinaryJarsResult> =
    wrapInFuture(jvmBinaryJarsResult)

  override fun workspaceBuildAndGetBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> =
    wrapInFuture(workspaceBuildTargetsResultAndBuild)

  override fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): CompletableFuture<WorkspaceBuildTargetsResult> =
    wrapInFuture(workspaceBuildTargetsPartial)

  override fun workspaceBuildTargetsFirstPhase(
    params: WorkspaceBuildTargetsFirstPhaseParams,
  ): CompletableFuture<WorkspaceBuildTargetsResult> = wrapInFuture(workspaceBuildTargetsFirstPhase)

  override fun buildTargetPythonOptions(params: PythonOptionsParams): CompletableFuture<PythonOptionsResult> =
    wrapInFuture(pythonOptionsResult)

  override fun rustWorkspace(params: RustWorkspaceParams): CompletableFuture<RustWorkspaceResult> = wrapInFuture(rustWorkspaceResult)

  override fun bazelResolveLocalToRemote(params: BazelResolveLocalToRemoteParams): CompletableFuture<BazelResolveLocalToRemoteResult> =
    wrapInFuture(bazelResolveLocalToRemote)

  override fun bazelResolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): CompletableFuture<BazelResolveRemoteToLocalResult> =
    wrapInFuture(bazelResolveRemoteToLocal)

  override fun workspaceBazelRepoMapping(): CompletableFuture<WorkspaceBazelRepoMappingResult> =
    wrapInFuture(workspaceBazelRepoMappingResult)

  private fun <T> wrapInFuture(value: T?): CompletableFuture<T> =
    value?.let { CompletableFuture.completedFuture(it) } ?: CompletableFuture.failedFuture(Exception("mock value is null"))
}
