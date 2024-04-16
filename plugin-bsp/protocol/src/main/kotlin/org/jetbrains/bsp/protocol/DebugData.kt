package org.jetbrains.bsp.protocol

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import ch.epfl.scala.bsp4j.StatusCode

public data class AnalysisDebugParams(
  val originId: String,
  val port: Int,
  val targets: List<BuildTargetIdentifier>,
)

public data class AnalysisDebugResult(
  val originId: String,
  val statusCode: StatusCode,
)

public data class RemoteDebugData(
  val debugType: String,
  val port: Int,
)

public data class RunWithDebugParams(
  val originId: String,
  val runParams: RunParams,
  val debug: RemoteDebugData?,
)
