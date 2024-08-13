package org.jetbrains.plugins.bsp.run

import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.MessageEvent

internal typealias TaskId = String

public interface BspTaskListener {
  public fun onDiagnostic(
    textDocument: String,
    buildTarget: String,
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
    message: String,
    status: StatusCode,
    data: Any?,
  ) {}

  public fun onLogMessage(message: String) {}

  public fun onShowMessage(message: String) {}
}
