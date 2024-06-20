package org.jetbrains.bazel.debug.connector

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.debug.console.StarlarkDebugTaskListener
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointHandler
import org.jetbrains.bazel.debug.platform.StarlarkDebugProcess
import org.jetbrains.bsp.protocol.AnalysisDebugResult
import java.util.concurrent.CompletableFuture

class StarlarkDebugSessionManager(private val project: Project) {
  val taskListener = StarlarkDebugTaskListener(project)
  private var futureToCancelOnStop: CompletableFuture<AnalysisDebugResult>? = null
  private var messenger: StarlarkDebugMessenger? = null
  private var eventHandler: ThreadAwareEventHandler? = null

  private val loop = createLoop()

  private fun startDebuggingProcess(session: XDebugSession, debugPort: Int): StarlarkDebugProcess {
    val connector = StarlarkSocketConnector.tryConnectTo(
      debugPort,
      Registry.intValue("bazel.starlark.debug.socket.attempts"),
      Registry.intValue("bazel.starlark.debug.socket.interval").toLong(),
    )

    return StarlarkDebugMessenger(connector, session).let {
      messenger = it
      val breakpointHandler = StarlarkBreakpointHandler(it)
      eventHandler = ThreadAwareEventHandler(session, it, breakpointHandler::getBreakpointByPathAndLine)
      ProgressManager.getInstance().run(loop)
      val debugProcess =
        StarlarkDebugProcess(connector, session, breakpointHandler, taskListener.console)
      taskListener.replaceSuspendChecker { debugProcess.isSuspended() }
      return@let debugProcess
    }
  }

  /** This function needs to be safe to call multiple times */
  private fun stop() {
    futureToCancelOnStop?.cancel(true)
    messenger?.close()
  }

  private fun XDebugProcess.isSuspended(): Boolean =
    this.session
      // if the top frame is not available, it is not a normal suspension
      ?.let { it.isPaused && !it.isStopped && it.suspendContext?.activeExecutionStack?.topFrame != null }
      ?: true

  fun registerFutureToStop(future: CompletableFuture<AnalysisDebugResult>) {
    // if for some reason, another future is registered, cancel the previous one
    futureToCancelOnStop?.cancel(true)
    futureToCancelOnStop = future
  }

  @TestOnly
  internal fun startDryAndGetLoopIteration(
    messenger: StarlarkDebugMessenger,
    eventHandler: ThreadAwareEventHandler,
  ): () -> Unit {
    this.messenger = messenger
    this.eventHandler = eventHandler
    return this.loop::performEventLoopIteration
  }

  private enum class State {
    READY,
    RUNNING,
    DISPOSED,
  }

  private fun createLoop() =
    object : Task.Backgroundable(project, BazelPluginBundle.message("starlark.debug.task.title")) {
      private var state = State.READY

      override fun run(indicator: ProgressIndicator) {
        state = State.RUNNING
        while (state == State.RUNNING) {
          performEventLoopIteration()
        }
      }

      override fun onCancel() {
        stopLoop()
        super.onCancel()
      }

      @VisibleForTesting
      fun performEventLoopIteration() {
        try {
          eventHandler?.let {
            messenger?.readEventAndHandle(it)
          }
        } catch (_: Exception) {
          stopLoop()
        }
      }

      /** This function needs to be safe to call multiple times */
      fun stopLoop() {
        if (state != State.DISPOSED) {
          state = State.DISPOSED
          this@StarlarkDebugSessionManager.stop()
        }
      }
    }

  inner class ProcessStarter(private val port: Int) : XDebugProcessStarter() {
    override fun start(session: XDebugSession): XDebugProcess =
      startDebuggingProcess(session, port)
  }
}
