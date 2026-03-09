package org.jetbrains.bazel.ui.console

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jediterm.core.util.TermSize
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bsp.protocol.TaskId
import java.util.concurrent.TimeoutException

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
  } catch (ex: Throwable) {
    val result = if (registerException(subtaskId, ex)) {
      when (ex) {
        is java.util.concurrent.CancellationException ->
          FailureResultImpl(BazelPluginBundle.message("console.task.exception.cancellation.message"))

        is TimeoutException ->
          FailureResultImpl(BazelPluginBundle.message("console.task.exception.timeout.message"))

        else ->
          FailureResultImpl(ex)
      }
    } else {
      FailureResultImpl()
    }
    finishSubtask(subtaskId, result = result)
    throw ex
  }
}

internal suspend fun <T> TaskConsole.withSubtask(
  reporter: SequentialProgressReporter,
  subtaskId: TaskId,
  text: String,
  block: suspend (subtaskId: TaskId) -> T,
) {
  reporter.indeterminateStep(text) {
    withSubtask(
      subtaskId = subtaskId,
      message = text,
      block = block,
    )
  }
}
