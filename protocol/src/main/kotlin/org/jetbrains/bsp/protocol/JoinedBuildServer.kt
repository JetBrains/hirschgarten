package org.jetbrains.bsp.protocol

import java.util.concurrent.CompletableFuture

interface JoinedBuildServer {
  fun buildInitialize(params: InitializeBuildParams): CompletableFuture<InitializeBuildResult>

  fun onBuildInitialized()

  fun buildShutdown(): CompletableFuture<Any>

  fun onBuildExit()

  fun workspaceBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult>

  fun workspaceReload(): CompletableFuture<Any>

  fun buildTargetSources(params: SourcesParams): CompletableFuture<SourcesResult>

  fun buildTargetInverseSources(params: InverseSourcesParams): CompletableFuture<InverseSourcesResult>

  fun buildTargetDependencySources(params: DependencySourcesParams): CompletableFuture<DependencySourcesResult>

  fun buildTargetDependencyModules(params: DependencyModulesParams): CompletableFuture<DependencyModulesResult>

  fun buildTargetResources(params: ResourcesParams): CompletableFuture<ResourcesResult>

  fun buildTargetOutputPaths(params: OutputPathsParams): CompletableFuture<OutputPathsResult>

  fun buildTargetCompile(params: CompileParams): CompletableFuture<CompileResult>

  fun buildTargetRun(params: RunParams): CompletableFuture<RunResult>

  fun buildTargetTest(params: TestParams): CompletableFuture<TestResult>

  fun buildTargetCleanCache(params: CleanCacheParams): CompletableFuture<CleanCacheResult>

  fun onRunReadStdin(params: ReadParams)

  fun bazelResolveLocalToRemote(params: BazelResolveLocalToRemoteParams): CompletableFuture<BazelResolveLocalToRemoteResult>

  fun bazelResolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): CompletableFuture<BazelResolveRemoteToLocalResult>

  fun buildTargetJvmTestEnvironment(params: JvmTestEnvironmentParams): CompletableFuture<JvmTestEnvironmentResult>

  fun buildTargetJvmRunEnvironment(params: JvmRunEnvironmentParams): CompletableFuture<JvmRunEnvironmentResult>

  fun buildTargetJvmCompileClasspath(params: JvmCompileClasspathParams): CompletableFuture<JvmCompileClasspathResult>

  fun buildTargetScalacOptions(params: ScalacOptionsParams): CompletableFuture<ScalacOptionsResult>

  @Deprecated("")
  fun buildTargetScalaTestClasses(params: ScalaTestClassesParams): CompletableFuture<ScalaTestClassesResult>

  @Deprecated("")
  fun buildTargetScalaMainClasses(params: ScalaMainClassesParams): CompletableFuture<ScalaMainClassesResult>

  fun buildTargetJavacOptions(params: JavacOptionsParams): CompletableFuture<JavacOptionsResult>

  fun buildTargetCppOptions(params: CppOptionsParams): CompletableFuture<CppOptionsResult>

  fun buildTargetPythonOptions(params: PythonOptionsParams): CompletableFuture<PythonOptionsResult>

  fun rustWorkspace(params: RustWorkspaceParams): CompletableFuture<RustWorkspaceResult>

  fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult>

  fun workspaceGoLibraries(): CompletableFuture<WorkspaceGoLibrariesResult>

  /**
   * Returns the list of all targets in the workspace that are neither modules nor libraries, but should be displayed in the UI.
   */

  fun workspaceNonModuleTargets(): CompletableFuture<NonModuleTargetsResult>

  fun workspaceDirectories(): CompletableFuture<WorkspaceDirectoriesResult>

  fun workspaceInvalidTargets(): CompletableFuture<WorkspaceInvalidTargetsResult>

  fun buildTargetAnalysisDebug(params: AnalysisDebugParams): CompletableFuture<AnalysisDebugResult>

  fun buildTargetRunWithDebug(params: RunWithDebugParams): CompletableFuture<RunResult>

  fun buildTargetMobileInstall(params: MobileInstallParams): CompletableFuture<MobileInstallResult>

  fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): CompletableFuture<JvmBinaryJarsResult>

  fun workspaceBuildAndGetBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult>

  fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): CompletableFuture<WorkspaceBuildTargetsResult>

  fun workspaceBuildTargetsFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): CompletableFuture<WorkspaceBuildTargetsResult>

  fun workspaceBazelRepoMapping(): CompletableFuture<WorkspaceBazelRepoMappingResult>
}
