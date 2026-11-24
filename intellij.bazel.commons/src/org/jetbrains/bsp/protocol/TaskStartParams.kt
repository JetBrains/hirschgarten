package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class TaskStartParams(
  val taskId: TaskId,
  val eventTime: Long? = null,
  val message: String? = null,
  var data: TaskStartData? = null,
)

@ApiStatus.Internal
sealed interface TaskStartData
