package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

open class BazelProcessHandler(private val project: Project, private val runDeferred: Deferred<*>) : ProcessHandler() {
  private var runningTests = false

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
    if (runningTests) {
      // When there are no tests in the test console, it will wrongly show a cancelled run as "No tests were found".
      // For that reason, when a testing process is being destroyed, we send a fake "terminated tests"
      notifyTextAvailable(ServiceMessageBuilder.testSuiteStarted("Run cancelled").toString() + "\n", ProcessOutputType.STDOUT)
    }
    runDeferred.cancel()
  }

  override fun detachProcessImpl() {
    notifyProcessDetached()
  }

  override fun detachIsDefault(): Boolean = false

  override fun getProcessInput(): OutputStream? = null

  fun switchToTestingMode() {
    runningTests = true
  }
}
