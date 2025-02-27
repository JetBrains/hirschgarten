package org.jetbrains.bsp.protocol

import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.TaskId

data class PublishOutputParams(
  val originId: String,
  val taskId: TaskId?,
  val buildTarget: BuildTargetIdentifier?,
  val dataKind: String,
  val data: Any,
)
