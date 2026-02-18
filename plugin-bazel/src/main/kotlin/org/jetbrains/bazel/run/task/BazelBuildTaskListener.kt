package org.jetbrains.bazel.run.task

import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bazel.utils.findCanonicalVirtualFileThatExists
import org.jetbrains.bsp.protocol.CompileReport
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

class BazelBuildTaskListener(private val taskConsole: TaskConsole) : BazelTaskListener {
  override fun onTaskFinish(
    taskId: TaskId,
    message: String,
    status: BazelStatus,
    data: Any?,
  ) {
    when (data) {
      is CompileReport -> {
        if (data.errors == 0 && status == BazelStatus.SUCCESS) {
          taskConsole.finishSubtask(taskId, message, FailureResultImpl())
        } else if (status == BazelStatus.CANCEL) {
          taskConsole.finishSubtask(taskId, message, SkippedResultImpl())
        } else {
          taskConsole.finishSubtask(taskId, message, SuccessResultImpl())
        }
      }

      else -> taskConsole.finishSubtask(taskId, message, SuccessResultImpl())
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
      severity = severity,
    )
  }

  override fun onLogMessage(taskId: TaskId, message: String) {
    taskConsole.addMessage(taskId, message)
  }
}
