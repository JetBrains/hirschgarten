package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.TaskId

data class PublishOutputParams(
  val originId: String,
  val taskId: TaskId?,
  val buildTarget: Label?,
  val dataKind: String,
  val data: Any,
)
