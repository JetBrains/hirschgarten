package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.RunResult
import ch.epfl.scala.bsp4j.TestResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.util.concurrent.CompletableFuture

public interface BazelBuildServer {
  @JsonRequest("workspace/libraries")
  public fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult>

  @JsonRequest("workspace/goLibraries")
  public fun workspaceGoLibraries(): CompletableFuture<WorkspaceGoLibrariesResult>

  /**
   * Returns the list of all targets in the workspace that are neither modules nor libraries, but should be displayed in the UI.
   */
  @JsonRequest("workspace/nonModuleTargets")
  public fun workspaceNonModuleTargets(): CompletableFuture<NonModuleTargetsResult>

  @JsonRequest("workspace/directories")
  public fun workspaceDirectories(): CompletableFuture<WorkspaceDirectoriesResult>

  @JsonRequest("workspace/invalidTargets")
  public fun workspaceInvalidTargets(): CompletableFuture<WorkspaceInvalidTargetsResult>

  @JsonRequest("buildTarget/analysisDebug")
  public fun buildTargetAnalysisDebug(params: AnalysisDebugParams): CompletableFuture<AnalysisDebugResult>

  @JsonRequest("buildTarget/runWithDebug")
  public fun buildTargetRunWithDebug(params: RunWithDebugParams): CompletableFuture<RunResult>

  @JsonRequest("buildTarget/testWithDebug")
  public fun buildTargetTestWithDebug(params: TestWithDebugParams): CompletableFuture<TestResult>

  @JsonRequest("buildTarget/mobileInstall")
  public fun buildTargetMobileInstall(params: MobileInstallParams): CompletableFuture<MobileInstallResult>

  @JsonRequest("buildTarget/jvmBinaryJars")
  public fun buildTargetJvmBinaryJars(params: JvmBinaryJarsParams): CompletableFuture<JvmBinaryJarsResult>

  @JsonRequest("workspace/buildAndGetBuildTargets")
  public fun workspaceBuildAndGetBuildTargets(): CompletableFuture<WorkspaceBuildTargetsResult>

  @JsonRequest("workspace/buildTargetsPartial")
  public fun workspaceBuildTargetsPartial(params: WorkspaceBuildTargetsPartialParams): CompletableFuture<WorkspaceBuildTargetsResult>
}
