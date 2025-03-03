package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.coroutines.BspCoroutineService
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

open class BspProcessHandler(private val project: Project, private val runDeferred: Deferred<*>) : ProcessHandler() {
  override fun startNotify() {
    super.startNotify()
    runDeferred.invokeOnCompletion { e ->
      // invokeOnCompletion is called synchronously inside a canceled coroutine, we don't want that
      BspCoroutineService.getInstance(project).start {
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
