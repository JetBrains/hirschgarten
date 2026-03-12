package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
data class CompileParams(
  val taskId: TaskId,
  val targets: List<Label>,
  val arguments: List<String>? = null,
)
