package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.util.concurrent.CompletableFuture

public data class LibraryItem(
        val id: BuildTargetIdentifier,
        val dependencies: List<BuildTargetIdentifier>,
        val jars: List<String>,
        val capabilities: BuildTargetCapabilities
)

public data class WorkspaceLibrariesResult(
        val libraries: List<LibraryItem>
)

public data class LibraryDetails(
        val name: String,
        val roots: List<String>
)

public interface BazelBuildServer {
  @JsonRequest("workspace/libraries")
  public fun workspaceLibraries(): CompletableFuture<WorkspaceLibrariesResult>
}
