package org.jetbrains.bsp.protocol

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification

interface BazelBuildClient {
  @JsonNotification("build/publishOutput") fun onBuildPublishOutput(params: PublishOutputParams)
}
