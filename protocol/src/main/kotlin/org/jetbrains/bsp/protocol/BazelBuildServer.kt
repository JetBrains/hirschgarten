package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.RunResult
import ch.epfl.scala.bsp4j.TestResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.util.concurrent.CompletableFuture

interface BazelBuildServer {
  @JsonRequest("workspace/libraries")
  fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult>

  @JsonRequest("workspace/goLibraries")
  fun workspaceGoLibraries(): CompletableFuture<WorkspaceGoLibrariesResult>

  /**
   * Returns the list of all targets in the workspace that are neither modules nor libraries, but should be displayed in the UI.
   */
  @JsonRequest("workspace/nonModuleTargets")
  fun workspaceNonModuleTargets(): CompletableFuture<NonModuleTargetsResult>

  @JsonRequest("workspace/directories")
  fun workspaceDirectories(): CompletableFuture<WorkspaceDirectoriesResult>

  @JsonRequest("workspace/invalidTargets")
  fun workspaceInvalidTargets(): CompletableFuture<WorkspaceInvalidTargetsResult>

  @JsonRequest("buildTarget/analysisDebug")
  fun buildTargetAnalysisDebug(params: AnalysisDebugParams): CompletableFuture<AnalysisDebugResult>

  @JsonRequest("buildTarget/runWithDebug")
  fun buildTargetRunWithDebug(params: RunWithDebugParams): CompletableFuture<RunResult>

  @JsonRequest("buildTarget/testWithDebug")
  fun buildTargetTestWithDebug(params: TestWithDebugParams): CompletableFuture<TestResult>

  @JsonRequest("buildTarget/mobileInstall")
  fun buildTargetMobileInstall(params: MobileInstallParams): CompletableFuture<MobileInstallResult>

  @JsonRequest("buildTarget/jvmBinaryJars")
  fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): CompletableFuture<JvmBinaryJarsResult>

  @JsonRequest("workspace/buildAndGetBuildTargets")
  fun workspaceBuildAndGetBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult>

  @JsonRequest("workspace/buildTargetsPartial")
  fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): CompletableFuture<WorkspaceBuildTargetsResult>

  @JsonRequest("workspace/buildTargetsFirstPhase")
  fun workspaceBuildTargetsFirstPhase(params: WorkspaceBuildTargetsFirstPhaseParams): CompletableFuture<WorkspaceBuildTargetsResult>
}
