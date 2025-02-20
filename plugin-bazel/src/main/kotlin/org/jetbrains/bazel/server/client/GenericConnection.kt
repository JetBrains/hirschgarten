package org.jetbrains.bazel.server.client

import org.jetbrains.bsp.protocol.JoinedBuildServer

interface GenericConnection {
  val server: JoinedBuildServer

  fun shutdown()
}
