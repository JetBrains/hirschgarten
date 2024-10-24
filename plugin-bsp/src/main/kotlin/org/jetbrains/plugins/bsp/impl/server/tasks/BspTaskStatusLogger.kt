package org.jetbrains.plugins.bsp.impl.server.tasks

import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.impl.FailureResultImpl
import kotlinx.coroutines.Deferred
import org.jetbrains.plugins.bsp.building.TaskConsole
import org.jetbrains.plugins.bsp.config.BspPluginBundle
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
    StatusCode.OK -> bspBuildConsole.finishTask(uuid, BspPluginBundle.message("console.task.status.ok"))
    StatusCode.CANCELLED -> bspBuildConsole.finishTask(uuid, BspPluginBundle.message("console.task.status.cancelled"))
    StatusCode.ERROR ->
      bspBuildConsole.finishTask(
        uuid,
        BspPluginBundle.message("console.task.status.error"),
        FailureResultImpl(),
      )
    else -> bspBuildConsole.finishTask(uuid, BspPluginBundle.message("console.task.status.other"))
  }

  private fun catchBuildErrors(exception: Throwable?) {
    if (exception == null) return
    when (exception) {
      is CancellationException -> {
        bspBuildConsole.finishTask(
          originId,
          BspPluginBundle.message("console.task.exception.cancellation"),
          FailureResultImpl(BspPluginBundle.message("console.task.exception.cancellation.message")),
        )
      }
      is TimeoutException -> {
        bspBuildConsole.finishTask(
          originId,
          BspPluginBundle.message("console.task.exception.timed.out"),
          FailureResultImpl(BspPluginBundle.message("console.task.exception.timeout.message")),
        )
      }
      else -> {
        bspBuildConsole.finishTask(
          originId,
          BspPluginBundle.message("console.task.exception.other"),
          FailureResultImpl(exception),
        )
      }
    }
  }
}
