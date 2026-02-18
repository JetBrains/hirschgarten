package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label

data class AnalysisDebugParams(
  val taskId: TaskId,
  val port: Int,
  val targets: List<Label>,
)

data class AnalysisDebugResult(val statusCode: BazelStatus)

data class RunWithDebugParams(
  val taskId: TaskId,
  val runParams: RunParams,
  val debug: DebugType?,
)
