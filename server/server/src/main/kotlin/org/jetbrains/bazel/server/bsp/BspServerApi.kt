package org.jetbrains.bazel.server.bsp

import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectSyncService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bazel.workspacecontext.provider.DefaultWorkspaceContextProvider
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

class BspServerApi(
  private val projectSyncService: ProjectSyncService,
  private val executeService: ExecuteService,
  val workspaceContextProvider: DefaultWorkspaceContextProvider,
) : JoinedBuildServer {
  override suspend fun runSync(build: Boolean, originId: String): BazelProject = projectSyncService.runSync(build, originId)

  override suspend fun workspaceBuildTargets(): WorkspaceBuildTargetsResult = projectSyncService.workspaceBuildTargets()

  override suspend fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): WorkspaceBuildTargetsResult =
    projectSyncService.workspaceBuildTargetsPartial(
      targetsToSync = params.targets,
    )

  override suspend fun workspaceBuildTargetsFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): WorkspaceBuildTargetsResult =
    projectSyncService.workspaceBuildFirstPhase(params)

  override suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult =
    projectSyncService.buildTargetInverseSources(params)

  override suspend fun buildTargetDependencySources(params: DependencySourcesParams): DependencySourcesResult =
    projectSyncService.buildTargetDependencySources(params)

  override suspend fun buildTargetCompile(params: CompileParams): CompileResult = executeService.compile(params)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = executeService.analysisDebug(params)

  override suspend fun buildTargetTest(params: TestParams): TestResult = executeService.testWithDebug(params)

  override suspend fun buildTargetRun(params: RunParams): RunResult = executeService.run(params)

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = executeService.runWithDebug(params)

  override suspend fun buildTargetMobileInstall(params: MobileInstallParams): MobileInstallResult = executeService.mobileInstall(params)

  override suspend fun buildTargetJavacOptions(params: JavacOptionsParams): JavacOptionsResult =
    projectSyncService.buildTargetJavacOptions(params)

  override suspend fun buildTargetCppOptions(params: CppOptionsParams): CppOptionsResult = projectSyncService.buildTargetCppOptions(params)

  override suspend fun buildTargetJvmRunEnvironment(params: JvmRunEnvironmentParams): JvmRunEnvironmentResult =
    projectSyncService.jvmRunEnvironment(params)

  override suspend fun buildTargetJvmTestEnvironment(params: JvmTestEnvironmentParams): JvmTestEnvironmentResult =
    projectSyncService.jvmTestEnvironment(params)

  override suspend fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): JvmBinaryJarsResult = projectSyncService.jvmBinaryJars(params)

  override suspend fun workspaceLibraries(): WorkspaceLibrariesResult = projectSyncService.workspaceBuildLibraries()

  override suspend fun workspaceDirectories(): WorkspaceDirectoriesResult = projectSyncService.workspaceDirectories()

  override suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult = projectSyncService.workspaceBazelRepoMapping()

  override suspend fun workspaceBazelPaths(): WorkspaceBazelPathsResult = projectSyncService.workspaceBazelPaths()

  override suspend fun workspaceName(): WorkspaceNameResult = projectSyncService.workspaceName()

  override suspend fun bazelResolveLocalToRemote(params: BazelResolveLocalToRemoteParams): BazelResolveLocalToRemoteResult =
    projectSyncService.resolveLocalToRemote(params)

  override suspend fun bazelResolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): BazelResolveRemoteToLocalResult =
    projectSyncService.resolveRemoteToLocal(params)

  override suspend fun workspaceContext(): WorkspaceContext = projectSyncService.workspaceContext()

  override suspend fun jvmToolchainInfo() = projectSyncService.buildJvmToolchainInfo()
}
