package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class LogMessageParams(
  val task: TaskId,
  val type: MessageType,
  val message: String,
)
