package org.jetbrains.bsp.protocol

data class TaskStartParams(
  val taskId: TaskId,
  val originId: String,
  val eventTime: Long? = null,
  val message: String? = null,
  var data: TaskStartData? = null,
)

sealed interface TaskStartData

data class TestStart(val displayName: String, val location: Location? = null) : TaskStartData

data class TestTask(val target: BuildTargetIdentifier) : TaskStartData

data class CompileTask(val target: BuildTargetIdentifier) : TaskStartData
