package org.jetbrains.bsp.testkit.client

import org.jetbrains.bazel.server.connection.Connection
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.nio.file.Path

/**
 * A session is a "physical" connection to a BSP server. It must be closed when it is no longer
 * needed. The user is responsible for maintaining the correct BSP life-cycle.
 */
class Session(val workspacePath: Path, val client: MockClient) {
  val connection =
    Connection(
      installationDirectory = workspacePath,
      projectViewFile = null,
      workspace = workspacePath,
      client = client,
    )

  val server: JoinedBuildServer = connection.server
}
