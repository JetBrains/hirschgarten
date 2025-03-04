package org.jetbrains.bazel.taskEvents

import com.intellij.build.events.MessageEvent
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.StatusCode
import java.nio.file.Path

typealias TaskId = String

public interface BspTaskListener {
  public fun onDiagnostic(
    textDocument: String,
    buildTarget: Label,
    line: Int,
    character: Int,
    severity: MessageEvent.Kind,
    message: String,
  ) {
  }

  public fun onOutputStream(taskId: TaskId?, text: String) {}

  public fun onErrorStream(taskId: TaskId?, text: String) {}

  public fun onTaskStart(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    data: Any?,
  ) {}

  public fun onTaskProgress(
    taskId: TaskId,
    message: String,
    data: Any?,
  ) {}

  public fun onTaskFinish(
    taskId: TaskId,
    parentId: TaskId?,
    message: String,
    status: StatusCode,
    data: Any?,
  ) {}

  public fun onLogMessage(message: String) {}

  public fun onShowMessage(message: String) {}

  public fun onPublishCoverageReport(coverageReport: Path) {}
}
