package org.jetbrains.bazel.run.task

import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.TaskId
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bazel.utils.findCanonicalVirtualFileThatExists
import org.jetbrains.bsp.protocol.CompileReport
import java.nio.file.Path

class BazelBuildTaskListener(private val buildConsole: TaskConsole, private val originId: String) : BazelTaskListener {
  override fun onTaskStart(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    data: Any?,
  ) {
    if (parentId == null) {
      buildConsole.startSubtask(originId, taskId, message)
    } else {
      buildConsole.startSubtask(taskId, parentId, message)
    }
  }

  override fun onTaskProgress(
    taskId: TaskId,
    message: String,
    data: Any?,
  ) {
    buildConsole.addMessage(taskId, message)
  }

  override fun onTaskFinish(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    status: BazelStatus,
    data: Any?,
  ) {
    when (data) {
      is CompileReport -> {
        if (data.errors == 0 && status == BazelStatus.SUCCESS) {
          buildConsole.finishSubtask(taskId, message, FailureResultImpl())
        } else if (status == BazelStatus.CANCEL) {
          buildConsole.finishSubtask(taskId, message, SkippedResultImpl())
        } else {
          buildConsole.finishSubtask(taskId, message, SuccessResultImpl())
        }
      }

      else -> buildConsole.finishSubtask(taskId, message, SuccessResultImpl())
    }
  }

  override fun onDiagnostic(
    textDocument: Path?,
    buildTarget: Label,
    line: Int,
    character: Int,
    severity: MessageEvent.Kind,
    message: String,
  ) {
    buildConsole.addDiagnosticMessage(
      taskId = originId,
      path = textDocument
        ?.findCanonicalVirtualFileThatExists()
        ?.toNioPath()
        ?: textDocument,
      line = line,
      column = character,
      message = message,
      severity = severity,
    )
  }

  override fun onLogMessage(message: String) {
    buildConsole.addMessage(originId, message)
  }
}
