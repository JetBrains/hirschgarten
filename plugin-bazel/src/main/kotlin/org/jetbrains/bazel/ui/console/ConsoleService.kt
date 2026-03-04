package org.jetbrains.bazel.ui.console

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jediterm.core.util.TermSize
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.TaskId

@ApiStatus.Internal
interface ConsoleService {
  val buildConsole: TaskConsole
  val syncConsole: TaskConsole

  fun ptyTermSize(taskId: TaskId): TermSize? {
    val buildConsole = buildConsole
    val syncConsole = syncConsole
    if (buildConsole is PtyAwareTaskConsole) {
      buildConsole.ptyTermSize(taskId)?.let { return it }
    }
    if (syncConsole is PtyAwareTaskConsole) {
      syncConsole.ptyTermSize(taskId)?.let { return it }
    }
    return null
  }

  companion object {
    fun getInstance(project: Project): ConsoleService = project.service()
  }
}

internal val Project.syncConsole: TaskConsole
  get() = ConsoleService.getInstance(this).syncConsole

internal suspend fun <T> TaskConsole.withSubtask(
  subtaskId: TaskId,
  message: String,
  block: suspend (subtaskId: TaskId) -> T,
): T {
  startSubtask(subtaskId, message)
  try {
    val result = block(subtaskId)
    finishSubtask(subtaskId)
    return result
  }
  catch (ex: Throwable) {
    finishSubtask(subtaskId, result = FailureResultImpl(ex))
    throw ex
  }
}

internal suspend fun <T> Project.withSubtask(
  reporter: SequentialProgressReporter,
  subtaskId: TaskId,
  text: String,
  block: suspend (subtaskId: TaskId) -> T,
) {
  reporter.indeterminateStep(text) {
    syncConsole.withSubtask(
      subtaskId = subtaskId,
      message = text,
      block = block,
    )
  }
}
