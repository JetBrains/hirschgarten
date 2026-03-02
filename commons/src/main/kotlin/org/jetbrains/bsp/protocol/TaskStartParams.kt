package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

@ApiStatus.Internal
data class TaskStartParams(
  val taskId: TaskId,
  val eventTime: Long? = null,
  val message: String? = null,
  var data: TaskStartData? = null,
)

@ApiStatus.Internal
sealed interface TaskStartData

@ApiStatus.Internal
data class TestStart(val displayName: String, val isSuit: Boolean, val locationHint: String? = null) : TaskStartData

@ApiStatus.Internal
data class TestTask(val target: Label) : TaskStartData

@ApiStatus.Internal
data class CompileTask(val target: Label) : TaskStartData
