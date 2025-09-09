package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asCompletableFuture
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sdkcompat.HasDeferredPid
import org.jetbrains.bazel.sdkcompat.ProcessHandlerCompat
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.cancellation.CancellationException

open class BazelProcessHandler(
  private val project: Project,
  private val runDeferred: Deferred<*>,
  override val pid: Deferred<Long?>? = null,
) : ProcessHandlerCompat(),
  HasDeferredPid {
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

  protected override fun getNativePidCompat(): CompletableFuture<Long?>? = pid?.asCompletableFuture()
}
