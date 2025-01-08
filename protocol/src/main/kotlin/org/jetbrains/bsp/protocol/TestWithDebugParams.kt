package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.TestParams

data class TestWithDebugParams(
  val originId: String,
  val testParams: TestParams,
  val debug: RemoteDebugData?,
)
