package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
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

public interface BazelBuildServer {
  @JsonRequest("workspace/libraries")
  public fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult>

  @JsonRequest("workspace/directories")
  public fun workspaceDirectories(): CompletableFuture<WorkspaceDirectoriesResult>
}
