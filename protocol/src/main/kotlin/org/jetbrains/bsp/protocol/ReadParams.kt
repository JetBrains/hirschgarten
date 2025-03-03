package org.jetbrains.bsp.protocol

data class ReadParams(
  val originId: String,
  val task: TaskId? = null,
  val message: String,
)
