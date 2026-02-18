package org.jetbrains.bsp.protocol

data class LogMessageParams(
  val task: TaskId,
  val type: MessageType,
  val message: String,
)
