package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import kotlinx.coroutines.Deferred
import org.jetbrains.bazel.config.BspPluginBundle
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

open class BspProcessHandler(private val runDeferred: Deferred<*>) : ProcessHandler() {
  override fun startNotify() {
    super.startNotify()
    runDeferred.invokeOnCompletion { e ->
      when (e) {
        null -> {
          notifyProcessTerminated(0)
        }
        is CancellationException -> {
          notifyTextAvailable(BspPluginBundle.message("console.task.run.cancelled"), ProcessOutputType.STDERR)
          notifyProcessTerminated(1)
        }
        else -> {
          notifyTextAvailable(e.toString(), ProcessOutputType.STDERR)
          notifyProcessTerminated(1)
        }
      }
    }
  }

  override fun destroyProcessImpl() {
    runDeferred.cancel()
    notifyProcessTerminated(1)
  }

  override fun detachProcessImpl() {
    notifyProcessDetached()
  }

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null
}
