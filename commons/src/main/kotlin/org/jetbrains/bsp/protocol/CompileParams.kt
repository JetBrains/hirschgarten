package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class CompileParams(
  val taskId: TaskId,
  val targets: List<Label>,
  val arguments: List<String>? = null,
)
