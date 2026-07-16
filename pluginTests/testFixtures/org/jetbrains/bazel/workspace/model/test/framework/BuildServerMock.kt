package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.orFallbackVersion
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.CompileParams
import org.jetbrains.bsp.protocol.CompileResult
import org.jetbrains.bsp.protocol.InverseSourcesParams
import org.jetbrains.bsp.protocol.InverseSourcesResult
import org.jetbrains.bsp.protocol.JvmToolchainInfo
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunResult
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TestParams
import org.jetbrains.bsp.protocol.TestResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import java.nio.file.Paths
import kotlin.io.path.Path

open class BuildServerMock(
  private val inverseSourcesResult: InverseSourcesResult? = null,
  private val compileResult: CompileResult? = null,
  private val runResult: RunResult? = null,
  private val testResult: TestResult? = null,
  private val workspaceDirectoriesResult: WorkspaceDirectoriesResult = WorkspaceDirectoriesResult(listOf(), listOf()),
  private val analysisDebugResult: AnalysisDebugResult? = null,
  private val aspectSyncProject: AspectSyncProject? = null,
  private val phasedSyncProjectResult: PhasedSyncProject? = null,
) : BazelServerFacade {
  override suspend fun workspaceBuildTargets(params: WorkspaceBuildTargetParams): AspectSyncProject =
    wrapInFuture(aspectSyncProject)

  override suspend fun workspaceBuildPhasedTargets(params: WorkspaceBuildTargetPhasedParams): PhasedSyncProject =
    wrapInFuture(phasedSyncProjectResult)

  override suspend fun buildTargetInverseSources(inverseSourcesParams: InverseSourcesParams): InverseSourcesResult =
    wrapInFuture(inverseSourcesResult)

  override suspend fun buildTargetCompile(compileParams: CompileParams): CompileResult = wrapInFuture(compileResult)

  override suspend fun buildTargetRun(runParams: RunParams): RunResult = wrapInFuture(runResult)

  override suspend fun buildTargetTest(testParams: TestParams): TestResult = wrapInFuture(testResult)

  override suspend fun workspaceDirectories(repoMapping: RepoMapping, taskId: TaskId): WorkspaceDirectoriesResult = wrapInFuture(workspaceDirectoriesResult)

  override suspend fun buildTargetAnalysisDebug(params: AnalysisDebugParams): AnalysisDebugResult = wrapInFuture(analysisDebugResult)

  override val projectView: ProjectView = ProjectView.EMPTY

  override val bazelInfo =
    BazelInfo(
      execRoot = Paths.get("bazel-out/exec"),
      outputBase = Paths.get("bazel-out"),
      workspaceRoot = Paths.get(""),
      bazelBin = Path("bazel-bin"),
      release = BazelRelease.fromReleaseString("release 6.0.0").orFallbackVersion(),
      false,
      true,
      emptyList(),
    )

  override val bazelPathsResolver: BazelPathsResolver
    get() = BazelPathsResolver(bazelInfo)

  override val outFileHardLinks: BazelOutFileHardLinks
    get() = BazelOutFileHardLinks.NONE

  override suspend fun jvmToolchainInfoForTarget(target: Label): JvmToolchainInfo =
    JvmToolchainInfo("/path/to/java/home", "/path/to/bazel/toolchain", emptyList())

  private fun <T> wrapInFuture(value: T?): T = value ?: error("mock value is null")
}
