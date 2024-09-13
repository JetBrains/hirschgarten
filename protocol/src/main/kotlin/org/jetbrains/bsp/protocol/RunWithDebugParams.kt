package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.RunParams

data class RunWithDebugParams(
  val originId: String,
  val runParams: RunParams,
  val debug: RemoteDebugData?,
)
