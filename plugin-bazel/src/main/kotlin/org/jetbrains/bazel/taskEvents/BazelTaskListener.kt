package org.jetbrains.bazel.taskEvents

import com.intellij.build.events.MessageEvent
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.StatusCode
import java.nio.file.Path

typealias TaskId = String

interface BazelTaskListener {
  fun onDiagnostic(
    textDocument: String,
    buildTarget: Label,
    line: Int,
    character: Int,
    severity: MessageEvent.Kind,
    message: String,
  ) {
  }

  fun onOutputStream(taskId: TaskId?, text: String) {}

  fun onErrorStream(taskId: TaskId?, text: String) {}

  fun onTaskStart(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    data: Any?,
  ) {
  }

  fun onTaskProgress(
    taskId: TaskId,
    message: String,
    data: Any?,
  ) {
  }

  fun onTaskFinish(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    status: StatusCode,
    data: Any?,
  ) {
  }

  fun onLogMessage(message: String) {}

  fun onShowMessage(message: String) {}

  fun onPublishCoverageReport(coverageReport: Path) {}
}
