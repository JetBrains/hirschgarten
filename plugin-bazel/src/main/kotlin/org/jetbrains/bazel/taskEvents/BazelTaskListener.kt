package org.jetbrains.bazel.taskEvents

import com.intellij.build.events.MessageEvent
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

interface BazelTaskListener {
  fun onDiagnostic(
    taskId: TaskId,
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
    message: String,
    data: Any?,
  ) {
  }

  fun onTaskFinish(
    taskId: TaskId,
    message: String,
    status: BazelStatus,
    data: Any?,
  ) {
  }

  fun onLogMessage(taskId: TaskId, message: String) {}
  fun onPublishCoverageReport(coverageReport: Path) {}
  fun onCachedTestLog(testLog: Path) {}
}
