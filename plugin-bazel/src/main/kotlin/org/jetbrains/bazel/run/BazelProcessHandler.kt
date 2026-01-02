package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.cancellation.CancellationException

open class BazelProcessHandler(
  private val project: Project,
  private val runDeferred: Deferred<*>,
  val pid: Deferred<Long?>? = null,
) : ProcessHandler() {
  override fun startNotify() {
    super.startNotify()
    runDeferred.invokeOnCompletion { e ->
      // invokeOnCompletion is called synchronously inside a canceled coroutine, we don't want that
      BazelCoroutineService.getInstance(project).start {
        when (e) {
          null -> {
            notifyProcessTerminated(0)
          }

          is CancellationException -> {
            notifyTextAvailable(BazelPluginBundle.message("console.task.run.cancelled"), ProcessOutputType.STDERR)
            notifyProcessTerminated(1)
          }

          else -> {
            notifyTextAvailable(e.toString(), ProcessOutputType.STDERR)
            notifyProcessTerminated(1)
          }
        }
      }
    }
  }

  override fun destroyProcessImpl() {
    runDeferred.cancel()
  }

  override fun detachProcessImpl() {
    notifyProcessDetached()
  }

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null

  final override fun getNativePid(): CompletableFuture<Long?>? =
    pid?.asCompletableFuture()
}
