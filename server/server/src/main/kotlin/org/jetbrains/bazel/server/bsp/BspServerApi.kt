package org.jetbrains.bazel.server.bsp

import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectSyncService
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
import org.jetbrains.bsp.protocol.WorkspaceBazelBinPathResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceGoLibrariesResult
import org.jetbrains.bsp.protocol.WorkspaceInvalidTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceLibrariesResult
import java.util.concurrent.CompletableFuture

class BspServerApi(private val bazelServicesBuilder: (JoinedBuildClient, InitializeBuildParams) -> BazelServices) : JoinedBuildServer {
  private lateinit var client: JoinedBuildClient
  private lateinit var serverLifetime: BazelBspServerLifetime
  private lateinit var runner: BspRequestsRunner

  private lateinit var projectSyncService: ProjectSyncService
  private lateinit var executeService: ExecuteService

  fun initialize(
    client: JoinedBuildClient,
    serverLifetime: BazelBspServerLifetime,
    runner: BspRequestsRunner,
  ) {
    this.client = client
    this.serverLifetime = serverLifetime
    this.runner = runner
  }

  private fun initializeServices(initializeBuildParams: InitializeBuildParams) {
    val serverContainer = bazelServicesBuilder(client, initializeBuildParams)
    this.projectSyncService = serverContainer.projectSyncService
    this.executeService = serverContainer.executeService
  }

  override fun buildInitialize(initializeBuildParams: InitializeBuildParams): CompletableFuture<Any> =
    runner.handleRequest(
      methodName = "build/initialize",
      supplier = {
        initializeServices(initializeBuildParams)
      },
      precondition = { runner.serverIsNotFinished(it) },
    )

  override fun onBuildInitialized() {
    runner.handleNotification("build/initialized") { serverLifetime.initialize() }
  }

  override fun buildShutdown(): CompletableFuture<Any> =
    runner.handleRequest(
      methodName = "build/shutdown",
      supplier = {
        serverLifetime.finish()
        Any()
      },
      precondition = { runner.serverIsInitialized(it) },
    )

  override fun onBuildExit() {
    runner.handleNotification("build/exit") { serverLifetime.forceFinish() }
  }

  override fun workspaceBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> =
    runner.handleRequest("workspace/buildTargets") {
      projectSyncService.workspaceBuildTargets(
        cancelChecker = it,
        build = false,
      )
    }

  override fun workspaceBuildAndGetBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult> =
    runner.handleRequest("workspace/buildAndGetBuildTargets") {
      projectSyncService.workspaceBuildTargets(
        cancelChecker = it,
        build = true,
      )
    }

  override fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): CompletableFuture<WorkspaceBuildTargetsResult> =
    runner.handleRequest("workspace/buildTargetsPartial") {
      projectSyncService.workspaceBuildTargetsPartial(
        cancelChecker = it,
        targetsToSync = params.targets.map { it.label() },
      )
    }

  override fun workspaceBuildTargetsFirstPhase(
    params: WorkspaceBuildTargetsFirstPhaseParams,
  ): CompletableFuture<WorkspaceBuildTargetsResult> =
    runner.handleRequest("workspace/buildTargetsFirstPhase", projectSyncService::workspaceBuildFirstPhase, params)

  override fun workspaceReload(): CompletableFuture<Any> = runner.handleRequest("workspace/reload", projectSyncService::workspaceReload)

  override fun buildTargetSources(params: SourcesParams): CompletableFuture<SourcesResult> =
    runner.handleRequest("buildTarget/sources", projectSyncService::buildTargetSources, params)

  override fun buildTargetInverseSources(params: InverseSourcesParams): CompletableFuture<InverseSourcesResult> =
    runner.handleRequest("buildTarget/inverseSources", projectSyncService::buildTargetInverseSources, params)

  override fun buildTargetDependencySources(params: DependencySourcesParams): CompletableFuture<DependencySourcesResult> =
    runner.handleRequest("buildTarget/dependencySources", projectSyncService::buildTargetDependencySources, params)

  override fun buildTargetResources(params: ResourcesParams): CompletableFuture<ResourcesResult> =
    runner.handleRequest("buildTarget/resources", projectSyncService::buildTargetResources, params)

  override fun buildTargetCompile(params: CompileParams): CompletableFuture<CompileResult> =
    runner.handleRequest("buildTarget/compile", executeService::compile, params)

  override fun buildTargetAnalysisDebug(params: AnalysisDebugParams): CompletableFuture<AnalysisDebugResult> =
    runner.handleRequest("buildTargetAnalysisDebug", executeService::analysisDebug, params)

  override fun buildTargetTest(params: TestParams): CompletableFuture<TestResult> =
    runner.handleRequest("buildTarget/test", executeService::testWithDebug, params)

  override fun buildTargetRun(params: RunParams): CompletableFuture<RunResult> =
    runner.handleRequest("buildTarget/run", executeService::run, params)

  override fun buildTargetRunWithDebug(params: RunWithDebugParams): CompletableFuture<RunResult> =
    runner.handleRequest("buildTarget/runWithDebug", executeService::runWithDebug, params)

  override fun buildTargetMobileInstall(params: MobileInstallParams): CompletableFuture<MobileInstallResult> =
    runner.handleRequest("buildTarget/mobileInstall", executeService::mobileInstall, params)

  override fun buildTargetCleanCache(params: CleanCacheParams): CompletableFuture<CleanCacheResult> =
    runner.handleRequest("buildTarget/cleanCache", executeService::clean, params)

  override fun onRunReadStdin(readParams: ReadParams) {}

  override fun buildTargetDependencyModules(params: DependencyModulesParams): CompletableFuture<DependencyModulesResult> =
    runner.handleRequest("buildTarget/dependencyModules", projectSyncService::buildTargetDependencyModules, params)

  override fun buildTargetOutputPaths(params: OutputPathsParams): CompletableFuture<OutputPathsResult> =
    runner.handleRequest("buildTarget/outputPaths", projectSyncService::buildTargetOutputPaths, params)

  override fun buildTargetScalacOptions(params: ScalacOptionsParams): CompletableFuture<ScalacOptionsResult> =
    runner.handleRequest("buildTarget/scalacOptions", projectSyncService::buildTargetScalacOptions, params)

  @Deprecated("Deprecated in BSP. Use buildTarget/jvmTestEnvironment instead")
  override fun buildTargetScalaTestClasses(params: ScalaTestClassesParams): CompletableFuture<ScalaTestClassesResult> =
    runner.handleRequest("buildTarget/scalaTestClasses", projectSyncService::buildTargetScalaTestClasses, params)

  @Deprecated("Deprecated in BSP. Use buildTarget/jvmRunEnvironment instead")
  override fun buildTargetScalaMainClasses(params: ScalaMainClassesParams): CompletableFuture<ScalaMainClassesResult> =
    runner.handleRequest("buildTarget/scalaMainClasses", projectSyncService::buildTargetScalaMainClasses, params)

  override fun buildTargetJavacOptions(javacOptionsParams: JavacOptionsParams): CompletableFuture<JavacOptionsResult> =
    runner.handleRequest("buildTarget/javacOptions", projectSyncService::buildTargetJavacOptions, javacOptionsParams)

  override fun buildTargetCppOptions(params: CppOptionsParams): CompletableFuture<CppOptionsResult> =
    runner.handleRequest("buildTarget/cppOptions", projectSyncService::buildTargetCppOptions, params)

  override fun buildTargetPythonOptions(params: PythonOptionsParams): CompletableFuture<PythonOptionsResult> =
    runner.handleRequest("buildTarget/pythonOptions", projectSyncService::buildTargetPythonOptions, params)

  override fun buildTargetJvmRunEnvironment(params: JvmRunEnvironmentParams): CompletableFuture<JvmRunEnvironmentResult> =
    runner.handleRequest("buildTarget/jvmRunEnvironment", projectSyncService::jvmRunEnvironment, params)

  override fun buildTargetJvmCompileClasspath(params: JvmCompileClasspathParams): CompletableFuture<JvmCompileClasspathResult> =
    runner.handleRequest("buildTarget/jvmCompileClasspath", projectSyncService::jvmCompileClasspath, params)

  override fun buildTargetJvmTestEnvironment(params: JvmTestEnvironmentParams): CompletableFuture<JvmTestEnvironmentResult> =
    runner.handleRequest("buildTarget/jvmTestEnvironment", projectSyncService::jvmTestEnvironment, params)

  override fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): CompletableFuture<JvmBinaryJarsResult> =
    runner.handleRequest("buildTarget/jvmBinaryJars", projectSyncService::jvmBinaryJars, params)

  override fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult> =
    runner.handleRequest("workspace/libraries", projectSyncService::workspaceBuildLibraries)

  override fun workspaceGoLibraries(): CompletableFuture<WorkspaceGoLibrariesResult> =
    runner.handleRequest("workspace/goLibraries", projectSyncService::workspaceBuildGoLibraries)

  override fun workspaceNonModuleTargets(): CompletableFuture<NonModuleTargetsResult> =
    runner.handleRequest("workspace/nonModuleTargets", projectSyncService::workspaceNonModuleTargets)

  override fun workspaceInvalidTargets(): CompletableFuture<WorkspaceInvalidTargetsResult> =
    runner.handleRequest("workspace/invalidTargets", projectSyncService::workspaceInvalidTargets)

  override fun workspaceDirectories(): CompletableFuture<WorkspaceDirectoriesResult> =
    runner.handleRequest("workspace/directories", projectSyncService::workspaceDirectories)

  override fun workspaceBazelRepoMapping(): CompletableFuture<WorkspaceBazelRepoMappingResult> =
    runner.handleRequest("workspace/bazelRepoMapping", projectSyncService::workspaceBazelRepoMapping)

  override fun workspaceBazelBinPath(): CompletableFuture<WorkspaceBazelBinPathResult> =
    runner.handleRequest("workspace/bazelBinPath", projectSyncService::workspaceBazelBinPath)

  override fun rustWorkspace(params: RustWorkspaceParams): CompletableFuture<RustWorkspaceResult> =
    runner.handleRequest("buildTarget/rustWorkspace", projectSyncService::rustWorkspace, params)

  override fun bazelResolveLocalToRemote(params: BazelResolveLocalToRemoteParams): CompletableFuture<BazelResolveLocalToRemoteResult> =
    runner.handleRequest("debug/resolveLocalToRemote", projectSyncService::resolveLocalToRemote, params)

  override fun bazelResolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): CompletableFuture<BazelResolveRemoteToLocalResult> =
    runner.handleRequest("debug/resolveRemoteToLocal", projectSyncService::resolveRemoteToLocal, params)
}
