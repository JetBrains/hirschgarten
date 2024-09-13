package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.TestParams

data class TestWithDebugParams(
  val originId: String,
  val runParams: TestParams,
  val debug: RemoteDebugData?,
)
