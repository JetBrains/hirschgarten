package org.jetbrains.bazel.progress

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelExtBundle
import org.jetbrains.bsp.protocol.TaskId
import java.util.concurrent.TimeoutException

val Project.syncConsole: TaskConsole
  @ApiStatus.Internal
  get() = service<ConsoleService>().syncConsole

@ApiStatus.Internal
suspend fun <T> TaskConsole.withSubtask(
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
          FailureResultImpl(BazelExtBundle.message("console.task.exception.cancellation.message"))

        is TimeoutException ->
          FailureResultImpl(BazelExtBundle.message("console.task.exception.timeout.message"))

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

@ApiStatus.Internal
suspend fun <T> TaskConsole.withSubtask(
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
