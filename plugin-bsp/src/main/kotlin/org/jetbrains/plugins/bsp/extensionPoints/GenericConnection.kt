package org.jetbrains.plugins.bsp.extensionPoints

import org.jetbrains.bsp.protocol.JoinedBuildServer

interface GenericConnection {
  val server: JoinedBuildServer

  fun shutdown()
}
