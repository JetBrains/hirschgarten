package org.jetbrains.bsp.protocol

data class LogMessageParams(
  val type: MessageType,
  val task: TaskId? = null,
  val originId: String? = null,
  val message: String,
)
