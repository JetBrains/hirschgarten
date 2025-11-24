package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.ui.console.ids.PROJECT_SYNC_TASK_ID
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask

class SyncConsoleTask(
  private val project: Project,
  private val parentTaskId: String,
) {
  suspend fun <T> withTask(taskId: String, message: String, action: suspend SyncConsoleTask.() -> T): T {
    return project.syncConsole.withSubtask(this.parentTaskId, taskId, message) {
      action(SyncConsoleTask(project, taskId))
    }
  }
}

suspend fun <T> withTask(project: Project, taskId: String, message: String, action: suspend SyncConsoleTask.() -> T): T {
  return project.syncConsole.withSubtask(
    taskId = PROJECT_SYNC_TASK_ID,
    subtaskId = taskId,
    message = message,
    block = { action(SyncConsoleTask(project, taskId)) },
  )
}
