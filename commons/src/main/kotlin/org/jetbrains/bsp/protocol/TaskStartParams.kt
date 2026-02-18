package org.jetbrains.bsp.protocol

import org.jetbrains.bazel.label.Label

data class TaskStartParams(
  val taskId: TaskId,
  val eventTime: Long? = null,
  val message: String? = null,
  var data: TaskStartData? = null,
)

sealed interface TaskStartData

data class TestStart(val displayName: String, val isSuit: Boolean, val locationHint: String? = null) : TaskStartData

data class TestTask(val target: Label) : TaskStartData

data class CompileTask(val target: Label) : TaskStartData
