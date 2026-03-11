package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
data class AnalysisDebugParams(
  val taskId: TaskId,
  val port: Int,
  val targets: List<Label>,
)

@ApiStatus.Internal
data class AnalysisDebugResult(val statusCode: BazelStatus)

@ApiStatus.Internal
data class RunWithDebugParams(
  val taskId: TaskId,
  val runParams: RunParams,
  val debug: DebugType?,
)
