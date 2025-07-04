package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bsp.protocol.RunParams

data class AnalysisDebugParams(
  val originId: String,
  val port: Int,
  val targets: List<CanonicalLabel>,
)

data class AnalysisDebugResult(val statusCode: BazelStatus)

data class RunWithDebugParams(
  val originId: String,
  val runParams: RunParams,
  val debug: DebugType?,
)
