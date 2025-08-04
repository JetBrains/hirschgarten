package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.orLatestSupported
import org.jetbrains.bazel.label.Label
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
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
import org.jetbrains.bsp.protocol.WorkspaceBazelPathsResult
import org.jetbrains.bsp.protocol.WorkspaceBazelRepoMappingResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.bsp.protocol.WorkspaceNameResult
import org.jetbrains.bsp.protocol.WorkspacePhasedBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceTargetClasspathQueryParams
import java.nio.file.Paths
import kotlin.io.path.Path

open class BuildServerMock(
  private val bazelProject: BazelProject? = null,
  private val inverseSourcesResult: InverseSourcesResult? = null,
  private val compileResult: CompileResult? = null,
  private val runResult: RunResult? = null,
  private val testResult: TestResult? = null,
  private val workspaceDirectoriesResult: WorkspaceDirectoriesResult? = null,
  private val analysisDebugResult: AnalysisDebugResult? = null,
  private val runResultWithDebug: RunResult? = null,
  private val workspaceBazelRepoMappingResult: WorkspaceBazelRepoMappingResult? = null,
  private val workspaceContextResult: WorkspaceContext? = null,
  private val workspaceBuildTargetsResult: WorkspaceBuildTargetsResult? = null,
  private val workspacePhasedBuildTargetsResult: WorkspacePhasedBuildTargetsResult? = null,
  private val jvmClasspathResult: BspJvmClasspath? = null,
) : JoinedBuildServer {
  override suspend fun runSync(build: Boolean, originId: String): BazelProject = wrapInFuture(bazelProject)

  override suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): WorkspaceBuildTargetsResult =
    wrapInFuture(workspaceBuildTargetsResult)

  override suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): WorkspacePhasedBuildTargetsResult =
    wrapInFuture(workspacePhasedBuildTargetsResult)

  override suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult =
    wrapInFuture(inverseSourcesResult)

  override suspend fun buildTargetCompile(compileParams: CompileParams): CompileResult = wrapInFuture(compileResult)

  override suspend fun buildTargetRun(runParams: RunParams): RunResult = wrapInFuture(runResult)

  override suspend fun buildTargetTest(testParams: TestParams): TestResult = wrapInFuture(testResult)

  override suspend fun workspaceDirectories(): WorkspaceDirectoriesResult = wrapInFuture(workspaceDirectoriesResult)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = wrapInFuture(analysisDebugResult)

  override suspend fun buildTargetRunWithDebug(params: RunWithDebugParams): RunResult = wrapInFuture(runResultWithDebug)

  override suspend fun workspaceBazelRepoMapping(): WorkspaceBazelRepoMappingResult = wrapInFuture(workspaceBazelRepoMappingResult)

  override suspend fun workspaceBazelPaths(): WorkspaceBazelPathsResult {
    val bazelInfo =
      BazelInfo(
        execRoot = Paths.get(""),
        outputBase = Paths.get(""),
        workspaceRoot = Paths.get(""),
        bazelBin = Path("bazel-bin"),
        release = BazelRelease.fromReleaseString("release 6.0.0").orLatestSupported(),
        false,
        true,
        emptyList(),
      )
    return WorkspaceBazelPathsResult("/path/to/bazel-bin", "/path/to/bazel-out/exec", BazelPathsResolver(bazelInfo))
  }

  override suspend fun workspaceName(): WorkspaceNameResult = WorkspaceNameResult("_main")

  override suspend fun workspaceContext(): WorkspaceContext = wrapInFuture(workspaceContextResult)

  override suspend fun jvmToolchainInfo() = JvmToolchainInfo("/path/to/java/home", "/path/to/bazel/toolchain", emptyList())

  override suspend fun workspaceTargetClasspathQuery(params: WorkspaceTargetClasspathQueryParams): BspJvmClasspath =
    wrapInFuture(jvmClasspathResult)

  override suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo =
    JvmToolchainInfo("/path/to/java/home", "/path/to/bazel/toolchain", emptyList())

  private fun <T> wrapInFuture(value: T?): T = value ?: error("mock value is null")
}
