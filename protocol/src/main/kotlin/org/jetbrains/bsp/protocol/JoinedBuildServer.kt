package org.jetbrains.bsp.protocol

interface JoinedBuildServer {
  suspend fun workspaceBuildTargets(): WorkspaceBuildTargetsResult

  suspend fun buildTargetSources(params: SourcesParams): SourcesResult

  suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult

  suspend fun buildTargetDependencySources(params: DependencySourcesParams): DependencySourcesResult

  suspend fun buildTargetResources(params: ResourcesParams): ResourcesResult

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

  suspend fun buildTargetPythonOptions(params: PythonOptionsParams): PythonOptionsResult

  suspend fun workspaceLibraries(): WorkspaceLibrariesResult

  suspend fun workspaceGoLibraries(): WorkspaceGoLibrariesResult

  /**
   * Returns the list of all targets in the workspace that are neither modules nor libraries, but should be displayed in the UI.
   */

  suspend fun workspaceNonModuleTargets(): NonModuleTargetsResult

  suspend fun workspaceDirectories(): WorkspaceDirectoriesResult

  suspend fun workspaceInvalidTargets(): WorkspaceInvalidTargetsResult

  suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult

  suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult

  suspend fun buildTargetMobileInstall(params: MobileInstallParams): MobileInstallResult

  suspend fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult

  suspend fun workspaceBuildAndGetBuildTargets(): WorkspaceBuildTargetsResult

  suspend fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): WorkspaceBuildTargetsResult

  suspend fun workspaceBuildTargetsFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): WorkspaceBuildTargetsResult

  suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult

  suspend fun workspaceBazelBinPath(): WorkspaceBazelBinPathResult
}
