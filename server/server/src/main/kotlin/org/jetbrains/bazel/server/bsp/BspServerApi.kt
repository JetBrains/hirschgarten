package org.jetbrains.bazel.server.bsp

import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectSyncService
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteParams
import org.jetbrains.bsp.protocol.BazelResolveLocalToRemoteResult
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalParams
import org.jetbrains.bsp.protocol.BazelResolveRemoteToLocalResult
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
import org.jetbrains.bsp.protocol.JoinedBuildClient
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
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.RustWorkspaceParams
import org.jetbrains.bsp.protocol.RustWorkspaceResult
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

class BspServerApi(private val bazelServicesBuilder: suspend (JoinedBuildClient, InitializeBuildParams) -> BazelServices) :
  JoinedBuildServer {
  private lateinit var client: JoinedBuildClient
  private lateinit var serverLifetime: BazelBspServerLifetime

  private lateinit var projectSyncService: ProjectSyncService
  private lateinit var executeService: ExecuteService

  fun initialize(client: JoinedBuildClient, serverLifetime: BazelBspServerLifetime) {
    this.client = client
    this.serverLifetime = serverLifetime
  }

  private suspend fun initializeServices(initializeBuildParams: InitializeBuildParams) {
    val serverContainer = bazelServicesBuilder(client, initializeBuildParams)
    this.projectSyncService = serverContainer.projectSyncService
    this.executeService = serverContainer.executeService
  }

  override suspend fun buildInitialize(initializeBuildParams: InitializeBuildParams) {
    initializeServices(initializeBuildParams)
  }

  override suspend fun onBuildInitialized() {
    serverLifetime.initialize()
  }

  override suspend fun buildShutdown() {
    serverLifetime.finish()
  }

  override suspend fun onBuildExit() {
    serverLifetime.forceFinish()
  }

  override suspend fun workspaceBuildTargets(): WorkspaceBuildTargetsResult =
    projectSyncService.workspaceBuildTargets(
      build = false,
    )

  override suspend fun workspaceBuildAndGetBuildTargets(): WorkspaceBuildTargetsResult =
    projectSyncService.workspaceBuildTargets(
      build = true,
    )

  override suspend fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): WorkspaceBuildTargetsResult =
    projectSyncService.workspaceBuildTargetsPartial(
      targetsToSync = params.targets,
    )

  override suspend fun workspaceBuildTargetsFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): WorkspaceBuildTargetsResult =
    projectSyncService.workspaceBuildFirstPhase(params)

  override suspend fun buildTargetSources(params: SourcesParams): SourcesResult = projectSyncService.buildTargetSources(params)

  override suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult =
    projectSyncService.buildTargetInverseSources(params)

  override suspend fun buildTargetDependencySources(params: DependencySourcesParams): DependencySourcesResult =
    projectSyncService.buildTargetDependencySources(params)

  override suspend fun buildTargetResources(params: ResourcesParams): ResourcesResult = projectSyncService.buildTargetResources(params)

  override suspend fun buildTargetCompile(params: CompileParams): CompileResult = executeService.compile(params)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = executeService.analysisDebug(params)

  override suspend fun buildTargetTest(params: TestParams): TestResult = executeService.testWithDebug(params)

  override suspend fun buildTargetRun(params: RunParams): RunResult = executeService.run(params)

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = executeService.runWithDebug(params)

  override suspend fun buildTargetMobileInstall(params: MobileInstallParams): MobileInstallResult = executeService.mobileInstall(params)

  override suspend fun buildTargetDependencyModules(params: DependencyModulesParams): DependencyModulesResult =
    projectSyncService.buildTargetDependencyModules(params)

  override suspend fun buildTargetScalacOptions(params: ScalacOptionsParams): ScalacOptionsResult =
    projectSyncService.buildTargetScalacOptions(params)

  override suspend fun buildTargetJavacOptions(params: JavacOptionsParams): JavacOptionsResult =
    projectSyncService.buildTargetJavacOptions(params)

  override suspend fun buildTargetCppOptions(params: CppOptionsParams): CppOptionsResult = projectSyncService.buildTargetCppOptions(params)

  override suspend fun buildTargetPythonOptions(params: PythonOptionsParams): PythonOptionsResult =
    projectSyncService.buildTargetPythonOptions(params)

  override suspend fun buildTargetJvmRunEnvironment(params: JvmRunEnvironmentParams): JvmRunEnvironmentResult =
    projectSyncService.jvmRunEnvironment(params)

  override suspend fun buildTargetJvmCompileClasspath(params: JvmCompileClasspathParams): JvmCompileClasspathResult =
    projectSyncService.jvmCompileClasspath(params)

  override suspend fun buildTargetJvmTestEnvironment(params: JvmTestEnvironmentParams): JvmTestEnvironmentResult =
    projectSyncService.jvmTestEnvironment(params)

  override suspend fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult = projectSyncService.jvmBinaryJars(params)

  override suspend fun workspaceLibraries(): WorkspaceLibrariesResult = projectSyncService.workspaceBuildLibraries()

  override suspend fun workspaceGoLibraries(): WorkspaceGoLibrariesResult = projectSyncService.workspaceBuildGoLibraries()

  override suspend fun workspaceNonModuleTargets(): NonModuleTargetsResult = projectSyncService.workspaceNonModuleTargets()

  override suspend fun workspaceInvalidTargets(): WorkspaceInvalidTargetsResult = projectSyncService.workspaceInvalidTargets()

  override suspend fun workspaceDirectories(): WorkspaceDirectoriesResult = projectSyncService.workspaceDirectories()

  override suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult = projectSyncService.workspaceBazelRepoMapping()

  override suspend fun workspaceBazelBinPath(): WorkspaceBazelBinPathResult = projectSyncService.workspaceBazelBinPath()

  override suspend fun rustWorkspace(params: RustWorkspaceParams): RustWorkspaceResult = projectSyncService.rustWorkspace(params)

  override suspend fun bazelResolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult =
    projectSyncService.resolveLocalToRemote(params)

  override suspend fun bazelResolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult =
    projectSyncService.resolveRemoteToLocal(params)
}
