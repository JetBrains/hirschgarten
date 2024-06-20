package org.jetbrains.bazel.debug.configuration

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.net.NetUtils
import com.intellij.xdebugger.XDebuggerManager
import org.jdom.Element
import org.jetbrains.bazel.debug.connector.StarlarkDebugSessionManager
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import org.jetbrains.plugins.bsp.server.tasks.AnalysisDebugTask
import java.util.concurrent.CompletableFuture

class StarlarkDebugRunner : AsyncProgramRunner<StarlarkDebugRunner.Settings>() {
  override fun getRunnerId(): String = "StarlarkDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean =
    executorId == DefaultDebugExecutor.EXECUTOR_ID &&
      profile is StarlarkDebugConfiguration

  override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
    // cast will always succeed - canRun(...) makes sure only StarlarkDebugConfiguration is run with this runner
    val starlarkState = state as StarlarkDebugConfigurationState
    val project = environment.project
    val port = choosePort()
    val starlarkManager = StarlarkDebugSessionManager(project)
    val starter = starlarkManager.ProcessStarter(port)
    val target = BuildTargetIdentifier(starlarkState.target)

    val taskListener = starlarkManager.taskListener
    val task = AnalysisDebugTask(project, port, taskListener)

    val promise = AsyncPromise<RunContentDescriptor?>()

    ApplicationManager.getApplication().invokeLater {
      try {
        task.connectAndExecute(listOf(target)).apply {
          this relayResultTo starlarkState.futureProxy
          starlarkManager.registerFutureToStop(this)
        }
        promise.setResult(
          XDebuggerManager
            .getInstance(project)
            .startSession(environment, starter)
            .runContentDescriptor,
        )
      } catch (_: ProcessCanceledException) {
        // ignore
      } catch (e: ExecutionException) {
        promise.setError(e)
      }
    }
    return promise
  }

  class Settings : RunnerSettings {
    override fun readExternal(element: Element?) {
      // empty settings, don't do anything
    }

    override fun writeExternal(element: Element?) {
      // empty settings, don't do anything
    }
  }
}

private fun choosePort(): Int = NetUtils.findAvailableSocketPort()

private infix fun CompletableFuture<AnalysisDebugResult>.relayResultTo(other: CompletableFuture<AnalysisDebugResult>) {
  this.whenComplete { result, exception ->
    if (exception != null) {
      other.completeExceptionally(exception)
    } else {
      other.complete(result)
    }
  }
}
