package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildClient
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification

interface JoinedBuildClient : BuildClient {
  @JsonNotification("build/publishOutput")
  fun onBuildPublishOutput(params: PublishOutputParams)
}
