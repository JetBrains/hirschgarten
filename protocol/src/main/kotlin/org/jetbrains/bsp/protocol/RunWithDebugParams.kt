package org.jetbrains.bsp.protocol

import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.StatusCode

data class AnalysisDebugParams(
  val originId: String,
  val port: Int,
  val targets: List<BuildTargetIdentifier>,
)

data class AnalysisDebugResult(val statusCode: StatusCode)

data class RunWithDebugParams(
  val originId: String,
  val runParams: RunParams,
  val debug: RemoteDebugData?,
)
