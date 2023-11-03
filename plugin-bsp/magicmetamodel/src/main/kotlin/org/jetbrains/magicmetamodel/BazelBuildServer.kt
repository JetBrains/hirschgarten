package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.RunResult
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.util.concurrent.CompletableFuture

public data class LibraryItem(
  val id: BuildTargetIdentifier,
  val dependencies: List<BuildTargetIdentifier>,
  val jars: List<String>,
  val sourceJars: List<String>,
)

public data class WorkspaceLibrariesResult(
  val libraries: List<LibraryItem>,
)

public data class DirectoryItem(
  val uri: String,
)

public data class WorkspaceDirectoriesResult(
  val includedDirectories: List<DirectoryItem>,
  val excludedDirectories: List<DirectoryItem>,
)

public data class WorkspaceInvalidTargetsResult(
  val targets: List<BuildTargetIdentifier>,
)

public data class RemoteDebugData(
  val debugType: String,
  val port: Int,
)

public data class RunWithDebugParams(
  val originId: String,
  val runParams: RunParams,
  val debug: RemoteDebugData?,
)

public interface BazelBuildServer {
  @JsonRequest("workspace/libraries")
  public fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult>

  @JsonRequest("workspace/directories")
  public fun workspaceDirectories(): CompletableFuture<WorkspaceDirectoriesResult>

  @JsonRequest("workspace/invalidTargets")
  public fun workspaceInvalidTargets(): CompletableFuture<WorkspaceInvalidTargetsResult>

  @JsonRequest("buildTarget/runWithDebug")
  public fun buildTargetRunWithDebug(params: RunWithDebugParams): CompletableFuture<RunResult>
}
