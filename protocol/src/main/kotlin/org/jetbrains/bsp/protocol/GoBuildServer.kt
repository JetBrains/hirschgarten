package org.jetbrains.bsp.protocol

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import java.util.concurrent.CompletableFuture

interface GoBuildServer {
  @JsonRequest("buildTarget/goDebuggerData")
  fun goDebuggerData(): CompletableFuture<GoDebuggerDataResult>
}
