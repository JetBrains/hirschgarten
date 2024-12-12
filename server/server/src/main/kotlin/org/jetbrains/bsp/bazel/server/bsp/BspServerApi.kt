package org.jetbrains.bsp.bazel.server.bsp

import ch.epfl.scala.bsp4j.CleanCacheParams
import ch.epfl.scala.bsp4j.CleanCacheResult
import ch.epfl.scala.bsp4j.CompileParams
import ch.epfl.scala.bsp4j.CompileResult
import ch.epfl.scala.bsp4j.CppOptionsParams
import ch.epfl.scala.bsp4j.CppOptionsResult
import ch.epfl.scala.bsp4j.DebugSessionAddress
import ch.epfl.scala.bsp4j.DebugSessionParams
import ch.epfl.scala.bsp4j.DependencyModulesParams
import ch.epfl.scala.bsp4j.DependencyModulesResult
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.InitializeBuildParams
import ch.epfl.scala.bsp4j.InitializeBuildResult
import ch.epfl.scala.bsp4j.InverseSourcesParams
import ch.epfl.scala.bsp4j.InverseSourcesResult
import ch.epfl.scala.bsp4j.JavacOptionsParams
import ch.epfl.scala.bsp4j.JavacOptionsResult
import ch.epfl.scala.bsp4j.JvmCompileClasspathParams
import ch.epfl.scala.bsp4j.JvmCompileClasspathResult
import ch.epfl.scala.bsp4j.JvmRunEnvironmentParams
import ch.epfl.scala.bsp4j.JvmRunEnvironmentResult
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult
import ch.epfl.scala.bsp4j.OutputPathsParams
import ch.epfl.scala.bsp4j.OutputPathsResult
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.PythonOptionsResult
import ch.epfl.scala.bsp4j.ReadParams
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import ch.epfl.scala.bsp4j.RustWorkspaceParams
import ch.epfl.scala.bsp4j.RustWorkspaceResult
import ch.epfl.scala.bsp4j.ScalaMainClassesParams
import ch.epfl.scala.bsp4j.ScalaMainClassesResult
import ch.epfl.scala.bsp4j.ScalaTestClassesParams
import ch.epfl.scala.bsp4j.ScalaTestClassesResult
import ch.epfl.scala.bsp4j.ScalacOptionsParams
import ch.epfl.scala.bsp4j.ScalacOptionsResult
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.TestParams
import ch.epfl.scala.bsp4j.TestResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.bazel.server.sync.ExecuteService
import org.jetbrains.bsp.bazel.server.sync.ProjectSyncService
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.JvmBinaryJarsParams
import org.jetbrains.bsp.protocol.JvmBinaryJarsResult
import org.jetbrains.bsp.protocol.MobileInstallParams
import org.jetbrains.bsp.protocol.MobileInstallResult
import org.jetbrains.bsp.protocol.NonModuleTargetsResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestWithDebugParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
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

  private fun initializeServices(initializeBuildParams: InitializeBuildParams): InitializeBuildResult {
    val serverContainer = bazelServicesBuilder(client, initializeBuildParams)
    this.projectSyncService = serverContainer.projectSyncService
    this.executeService = serverContainer.executeService

    return projectSyncService.initialize()
  }

  override fun buildInitialize(initializeBuildParams: InitializeBuildParams): CompletableFuture<InitializeBuildResult> =
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
        targetsToSync = params.targets,
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
    runner.handleRequest("buildTarget/test", executeService::test, params)

  override fun buildTargetTestWithDebug(params: TestWithDebugParams): CompletableFuture<TestResult> =
    runner.handleRequest("buildTarget/testWithDebug", executeService::testWithDebug, params)

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

  override fun debugSessionStart(params: DebugSessionParams): CompletableFuture<DebugSessionAddress> {
    // TODO: https://youtrack.jetbrains.com/issue/BAZEL-239
    return CompletableFuture.failedFuture(Exception("This endpoint is not implemented yet"))
  }

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

  override fun rustWorkspace(params: RustWorkspaceParams): CompletableFuture<RustWorkspaceResult> =
    runner.handleRequest("buildTarget/rustWorkspace", projectSyncService::rustWorkspace, params)
}
