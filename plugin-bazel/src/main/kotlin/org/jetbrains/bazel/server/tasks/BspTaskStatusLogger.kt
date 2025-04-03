package org.jetbrains.bazel.server.tasks

import com.intellij.build.events.impl.FailureResultImpl
import kotlinx.coroutines.Deferred
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bsp.protocol.StatusCode
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeoutException

public class BspTaskStatusLogger<T>(
  private val taskDeferred: Deferred<T>,
  private val bspBuildConsole: TaskConsole,
  private val originId: String,
  private val statusCode: T.() -> StatusCode,
) {
  public suspend fun getResult(): T =
    taskDeferred
      .also { it.invokeOnCompletion { throwable -> catchBuildErrors(throwable) } }
      .await()
      .also { finishBuildConsoleTaskWithProperResult(it, bspBuildConsole, originId) }

  private fun finishBuildConsoleTaskWithProperResult(
    result: T,
    bspBuildConsole: TaskConsole,
    uuid: String,
  ) = when (result.statusCode()) {
    StatusCode.OK -> bspBuildConsole.finishTask(uuid, BazelPluginBundle.message("console.task.status.ok"))
    StatusCode.CANCELLED -> bspBuildConsole.finishTask(uuid, BazelPluginBundle.message("console.task.status.cancelled"))
    StatusCode.ERROR ->
      bspBuildConsole.finishTask(
        uuid,
        BazelPluginBundle.message("console.task.status.error"),
        FailureResultImpl(),
      )
    else -> bspBuildConsole.finishTask(uuid, BazelPluginBundle.message("console.task.status.other"))
  }

  private fun catchBuildErrors(exception: Throwable?) {
    if (exception == null) return
    when (exception) {
      is CancellationException -> {
        bspBuildConsole.finishTask(
          originId,
          BazelPluginBundle.message("console.task.exception.cancellation"),
          FailureResultImpl(BazelPluginBundle.message("console.task.exception.cancellation.message")),
        )
      }
      is TimeoutException -> {
        bspBuildConsole.finishTask(
          originId,
          BazelPluginBundle.message("console.task.exception.timed.out"),
          FailureResultImpl(BazelPluginBundle.message("console.task.exception.timeout.message")),
        )
      }
      else -> {
        bspBuildConsole.finishTask(
          originId,
          BazelPluginBundle.message("console.task.exception.other"),
          FailureResultImpl(exception),
        )
      }
    }
  }
}
