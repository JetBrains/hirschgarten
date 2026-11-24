package org.jetbrains.bazel.run.task

import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.utils.findCanonicalVirtualFileThatExists
import org.jetbrains.bsp.protocol.BuildFinished
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

@ApiStatus.Internal
class BazelBuildTaskListener(private val taskConsole: TaskConsole) : BazelTaskListener {
  override fun onTaskFinish(
    taskId: TaskId,
    message: String,
    status: BazelStatus,
    data: Any?,
  ) {
    when (data) {
      is BuildFinished -> finishBuildConsoleTaskWithProperResult(taskId, status, taskConsole)
    }
  }

  override fun onDiagnostic(
    taskId: TaskId,
    textDocument: Path?,
    buildTarget: Label,
    line: Int,
    character: Int,
    severity: MessageEvent.Kind,
    message: String,
    description: String?,
  ) {
    taskConsole.addDiagnosticMessage(
      taskId = taskId,
      path = textDocument
        ?.findCanonicalVirtualFileThatExists()
        ?.toNioPath()
        ?: textDocument,
      line = line,
      column = character,
      message = message,
      description = description,
      severity = severity,
    )
  }

  override fun onLogMessage(taskId: TaskId, message: String) {
    taskConsole.addMessage(taskId, message)
  }
}

@ApiStatus.Internal
fun finishBuildConsoleTaskWithProperResult(
  taskId: TaskId,
  status: BazelStatus,
  bspBuildConsole: TaskConsole,
): Unit = when (status) {
  BazelStatus.SUCCESS -> bspBuildConsole.finishTask(taskId, BazelBackendBundle.message("console.task.status.ok"))
  BazelStatus.CANCEL -> bspBuildConsole.finishTask(taskId, BazelBackendBundle.message("console.task.status.cancelled"))
  else ->
    bspBuildConsole.finishTask(
      taskId,
      BazelBackendBundle.message("console.task.status.error"),
      FailureResultImpl(),
    )
}
