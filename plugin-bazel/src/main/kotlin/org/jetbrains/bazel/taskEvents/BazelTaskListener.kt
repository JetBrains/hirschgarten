package org.jetbrains.bazel.taskEvents

import com.intellij.build.events.MessageEvent
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import java.nio.file.Path

typealias TaskId = String

interface BazelTaskListener {
  fun onDiagnostic(
    textDocument: Path?,
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

  fun onTaskFinish(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    status: BazelStatus,
    data: Any?,
  ) {
  }

  fun onLogMessage(message: String) {}

  fun onShowMessage(message: String) {}

  fun onPublishCoverageReport(coverageReport: Path) {}

  fun onCachedTestLog(testLog: Path) {}
}
