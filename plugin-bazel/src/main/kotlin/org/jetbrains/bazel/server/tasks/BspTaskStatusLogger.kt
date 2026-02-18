package org.jetbrains.bazel.server.tasks

import com.intellij.build.events.impl.FailureResultImpl
import kotlinx.coroutines.Deferred
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bsp.protocol.TaskId
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException

class BspTaskStatusLogger(
  private val taskDeferred: Deferred<BazelStatus>,
  private val bspBuildConsole: TaskConsole,
  private val taskId: TaskId,
) {
  suspend fun getStatus(): BazelStatus =
    taskDeferred
      .also { it.invokeOnCompletion { throwable -> catchBuildErrors(throwable) } }
      .await()
      .also { finishBuildConsoleTaskWithProperResult(it, bspBuildConsole) }

  private fun finishBuildConsoleTaskWithProperResult(
    status: BazelStatus,
    bspBuildConsole: TaskConsole,
  ) = when (status) {
    BazelStatus.SUCCESS -> bspBuildConsole.finishTask(taskId, BazelPluginBundle.message("console.task.status.ok"))
    BazelStatus.CANCEL -> bspBuildConsole.finishTask(taskId, BazelPluginBundle.message("console.task.status.cancelled"))
    else ->
      bspBuildConsole.finishTask(
        taskId,
        BazelPluginBundle.message("console.task.status.error"),
        FailureResultImpl(),
      )
  }

  private fun catchBuildErrors(exception: Throwable?) {
    if (exception == null) return
    when (exception) {
      is CancellationException -> {
        bspBuildConsole.finishTask(
          taskId,
          BazelPluginBundle.message("console.task.exception.cancellation"),
          FailureResultImpl(BazelPluginBundle.message("console.task.exception.cancellation.message")),
        )
      }
      is TimeoutException -> {
        bspBuildConsole.finishTask(
          taskId,
          BazelPluginBundle.message("console.task.exception.timed.out"),
          FailureResultImpl(BazelPluginBundle.message("console.task.exception.timeout.message")),
        )
      }
      else -> {
        bspBuildConsole.finishTask(
          taskId,
          BazelPluginBundle.message("console.task.exception.other"),
          FailureResultImpl(exception),
        )
      }
    }
  }
}
