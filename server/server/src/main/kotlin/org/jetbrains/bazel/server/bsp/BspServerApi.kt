package org.jetbrains.bazel.server.bsp

import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectSyncService
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.BazelProject
import org.jetbrains.bsp.protocol.BspJvmClasspath
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
import org.jetbrains.bsp.protocol.WorkspaceBazelPathsResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildPartialTargetsParams
import org.jetbrains.bsp.protocol.WorkspaceBuildPartialTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceNameResult
import org.jetbrains.bsp.protocol.WorkspacePhasedBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceTargetClasspathQueryParams

class BspServerApi(
  private val projectSyncService: ProjectSyncService,
  private val executeService: ExecuteService,
  val workspaceContext: WorkspaceContext,
  val bazelPathsResolver: BazelPathsResolver,
) : JoinedBuildServer {
  override suspend fun runSync(build: Boolean, originId: String): BazelProject = projectSyncService.runSync(build, originId)

  override suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): WorkspaceBuildTargetsResult =
    projectSyncService.workspaceBuildTargets(params)

  override suspend fun workspaceBuildTargetsPartial(params: WorkspaceBuildPartialTargetsParams): WorkspaceBuildPartialTargetsResult =
    projectSyncService.workspaceBuildTargetsPartial(params)

  override suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): WorkspacePhasedBuildTargetsResult =
    projectSyncService.workspaceBuildPhasedTargets(params)

  override suspend fun buildTargetInverseSources(params: InverseSourcesParams): InverseSourcesResult =
    projectSyncService.buildTargetInverseSources(params)

  override suspend fun buildTargetCompile(params: CompileParams): CompileResult = executeService.compile(params)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = executeService.analysisDebug(params)

  override suspend fun buildTargetTest(params: TestParams): TestResult = executeService.testWithDebug(params)

  override suspend fun buildTargetRun(params: RunParams): RunResult = executeService.run(params)

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = executeService.runWithDebug(params)

  override suspend fun workspaceDirectories(): WorkspaceDirectoriesResult = projectSyncService.workspaceDirectories()

  override suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult = projectSyncService.workspaceBazelRepoMapping()
  override suspend fun workspaceComputeBazelRepoMapping(): WorkspaceBazelRepoMappingResult =
    projectSyncService.workspaceComputeBazelRepoMapping()

  override suspend fun workspaceBazelPaths(): WorkspaceBazelPathsResult = projectSyncService.workspaceBazelPaths()

  override suspend fun workspaceName(): WorkspaceNameResult = projectSyncService.workspaceName()

  override suspend fun workspaceContext(): WorkspaceContext = projectSyncService.workspaceContext()

  override suspend fun workspaceTargetClasspathQuery(params: WorkspaceTargetClasspathQueryParams): BspJvmClasspath =
    projectSyncService.workspaceTargetClasspathQuery(params)

  override suspend fun jvmToolchainInfoForTarget(target: Label) = projectSyncService.buildJvmToolchainInfoForTarget(target)
}
