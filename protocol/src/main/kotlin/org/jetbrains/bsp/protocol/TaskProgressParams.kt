package org.jetbrains.bsp.protocol

data class TaskProgressParams(
  val taskId: TaskId,
  val originId: String,
  val eventTime: Long? = null,
  val message: String? = null,
  val total: Long? = null,
  val progress: Long? = null,
  val unit: String? = null,
)
