package org.jetbrains.bazel.debug.configuration

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.AsyncProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.net.NetUtils
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.jdom.Element
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.debug.connector.StarlarkDebugSessionManager
import org.jetbrains.bazel.debug.connector.StarlarkSocketConnector
import org.jetbrains.bazel.debug.console.StarlarkDebugTaskListener
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bsp.protocol.AnalysisDebugParams
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.Promise
import java.net.ConnectException
import java.util.UUID
import java.util.concurrent.CancellationException

class StarlarkDebugRunner : AsyncProgramRunner<StarlarkDebugRunner.Settings>() {
  override fun getRunnerId(): String = "StarlarkDebugRunner"

  override fun canRun(executorId: String, profile: RunProfile): Boolean =
    executorId == DefaultDebugExecutor.EXECUTOR_ID &&
      profile is StarlarkDebugConfiguration

  override fun execute(environment: ExecutionEnvironment, state: RunProfileState): Promise<RunContentDescriptor?> {
    val promise = AsyncPromise<RunContentDescriptor?>()
    BazelCoroutineService.getInstance(environment.project).start {
      executeAndCompletePromise(environment, state, promise)
    }
    return promise
  }

  private suspend fun executeAndCompletePromise(
    environment: ExecutionEnvironment,
    state: RunProfileState,
    promise: AsyncPromise<RunContentDescriptor?>,
  ) {
    // cast will always succeed - canRun(...) makes sure only StarlarkDebugConfiguration is run with this runner
    val starlarkState = state as StarlarkDebugConfigurationState
    val project = environment.project
    val port = choosePort()
    val starlarkManager = StarlarkDebugSessionManager(project)
    val target = Label.parseCanonical(starlarkState.target)

    val debugJob = connectAndExecuteAnalysisDebug(project, port, starlarkManager.taskListener, target, starlarkState.futureProxy)
    debugJob.invokeOnCompletion { _ -> Disposer.dispose(starlarkManager) }
    // Function connectAndExecuteAnalysisDebug(...) will not return until a server connection is established.
    //   Thanks to that, we are not trying to connect to the debug server while an auto-connect is still in progress.
    try {
      val connector = connectToDebugServer(project, port)
      val starter = starlarkManager.ProcessStarter(connector)
      starlarkManager.registerJobToCancel(debugJob)
      promise.setResult(
        XDebuggerManager
          .getInstance(project)
          .startSessionOnEDT(environment, starter)
          .runContentDescriptor,
      )
    } catch (e: CancellationException) {
      // user cancelled the connection attempt before it was completed
      debugJob.cancel(e)
      promise.setError(BazelPluginBundle.message("starlark.debug.exception.cancellation"))
    } catch (e: ConnectException) {
      // Socket failed to connect - Bazel server might be stuck waiting for debug connection.
      //   In that situation debugJob.cancel() will not stop the server, as we don't have the server PID yet.
      debugJob.cancel()
      promise.setError(BazelPluginBundle.message("starlark.debug.exception.connect"))
    } catch (e: Exception) {
      // unexpected error, we are not prepared for it - log it
      debugJob.cancel()
      promise.setError(e)
      log.error(e)
    }
  }

  private suspend fun connectAndExecuteAnalysisDebug(
    project: Project,
    port: Int,
    taskListener: StarlarkDebugTaskListener,
    target: CanonicalLabel,
    futureProxy: CompletableDeferred<AnalysisDebugResult>,
  ): Job =
    project.connection.runWithServer { server ->
      BazelCoroutineService.getInstance(project).start {
        analysisDebug(project, port, taskListener, target, server, futureProxy)
      }
    }

  private suspend fun analysisDebug(
    project: Project,
    port: Int,
    taskListener: StarlarkDebugTaskListener,
    target: CanonicalLabel,
    server: JoinedBuildServer,
    futureProxy: CompletableDeferred<AnalysisDebugResult>,
  ) {
    coroutineScope {
      val originId = "analysis-debug-" + UUID.randomUUID().toString()
      BazelTaskEventsService.getInstance(project).saveListener(originId, taskListener)
      val params = AnalysisDebugParams(originId, port, listOf(target))

      val buildDeferred = async { server.buildTargetAnalysisDebug(params) }
      try {
        val result = buildDeferred.await()
        futureProxy.complete(result)
      } catch (e: Exception) {
        buildDeferred.cancel()
        futureProxy.completeExceptionally(e)
      } finally {
        BazelTaskEventsService.getInstance(project).removeListener(originId)
      }
    }
  }

  @Throws(ConnectException::class, CancellationException::class)
  private suspend fun connectToDebugServer(project: Project, port: Int): StarlarkSocketConnector =
    withBackgroundProgress(project, BazelPluginBundle.message("starlark.debug.connecting.progress.name"), true) {
      StarlarkSocketConnector.tryConnectTo(
        port,
        Registry.intValue("bazel.starlark.debug.socket.attempts"),
        Registry.intValue("bazel.starlark.debug.socket.interval").toLong(),
      )
    }

  private suspend fun XDebuggerManager.startSessionOnEDT(
    environment: ExecutionEnvironment,
    starter: StarlarkDebugSessionManager.ProcessStarter,
  ): XDebugSession =
    withContext(Dispatchers.EDT) {
      startSession(environment, starter)
    }

  class Settings : RunnerSettings {
    override fun readExternal(element: Element?) {
      // empty settings, don't do anything
    }

    override fun writeExternal(element: Element?) {
      // empty settings, don't do anything
    }
  }

  private companion object {
    val log = logger<StarlarkDebugRunner>()
  }
}

private fun choosePort(): Int = NetUtils.findAvailableSocketPort()
