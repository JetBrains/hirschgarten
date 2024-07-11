package org.jetbrains.plugins.bsp.server.tasks

import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.impl.FailureResultImpl
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.reactToExceptionIn
import org.jetbrains.plugins.bsp.ui.console.TaskConsole
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

public class BspTaskStatusLogger<T>(
  private val taskFuture: CompletableFuture<T>,
  private val bspBuildConsole: TaskConsole,
  private val originId: String,
  private val cancelOn: CompletableFuture<Void>,
  private val statusCode: T.() -> StatusCode,
) {
  public fun getResult(): T = taskFuture
    .reactToExceptionIn(cancelOn)
    .catchBuildErrors()
    .get()
    .also { finishBuildConsoleTaskWithProperResult(it, bspBuildConsole, originId) }

  private fun finishBuildConsoleTaskWithProperResult(
    result: T,
    bspBuildConsole: TaskConsole,
    uuid: String,
  ) = when (result.statusCode()) {
    StatusCode.OK -> bspBuildConsole.finishTask(uuid, BspPluginBundle.message("console.task.status.ok"))
    StatusCode.CANCELLED -> bspBuildConsole.finishTask(uuid, BspPluginBundle.message("console.task.status.cancelled"))
    StatusCode.ERROR -> bspBuildConsole.finishTask(uuid,
      BspPluginBundle.message("console.task.status.error"), FailureResultImpl())
    else -> bspBuildConsole.finishTask(uuid, BspPluginBundle.message("console.task.status.other"))
  }

  private fun <T> CompletableFuture<T>.catchBuildErrors(): CompletableFuture<T> =
    this.whenComplete { _, exception ->
      exception?.let {
        if (isTimeoutException(it)) {
          val message = BspPluginBundle.message("console.task.exception.timeout.message")
          bspBuildConsole.finishTask(originId,
            BspPluginBundle.message("console.task.exception.timed.out"), FailureResultImpl(message)
          )
        } else if (isCancellationException(it)) {
          bspBuildConsole.finishTask(originId, BspPluginBundle.message("console.task.exception.cancellation"),
            FailureResultImpl(BspPluginBundle.message("console.task.exception.cancellation.message"))
          )
        } else {
          bspBuildConsole.finishTask(originId,
            BspPluginBundle.message("console.task.exception.other"), FailureResultImpl(it)
          )
        }
      }
    }

  private fun isTimeoutException(e: Throwable): Boolean =
    e is CompletionException && e.cause is TimeoutException

  private fun isCancellationException(e: Throwable): Boolean =
    e is CompletionException && e.cause is CancellationException
}
