package org.jetbrains.bsp.protocol

data class PrintParams(
  val originId: String,
  val task: TaskId,
  val message: String,
)
