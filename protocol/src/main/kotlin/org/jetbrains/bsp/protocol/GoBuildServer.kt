package org.jetbrains.bsp.protocol

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.util.concurrent.CompletableFuture

interface GoBuildServer {
  @JsonRequest("debug/resolveLocalToRemote")
  fun bazelResolveLocalToRemote(params: BazelResolveLocalToRemoteParams): CompletableFuture<BazelResolveLocalToRemoteResult>

  @JsonRequest("debug/resolveRemoteToLocal")
  fun bazelResolveRemoteToLocal(params: BazelResolveRemoteToLocalParams): CompletableFuture<BazelResolveRemoteToLocalResult>
}
