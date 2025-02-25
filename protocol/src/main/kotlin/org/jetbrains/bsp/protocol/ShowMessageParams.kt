package org.jetbrains.bsp.protocol

data class ShowMessageParams(
  val type: MessageType,
  val task: TaskId? = null,
  val originId: String,
  val message: String,
)
