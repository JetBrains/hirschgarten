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
import kotlin.concurrent.Volatile

class StarlarkDebugManager(private val project: Project) {
  val taskListener = StarlarkDebugTaskListener(project)
  private var futureToCancelOnStop: CompletableFuture<AnalysisDebugResult>? = null

  @Volatile private var state = State.READY
  private var classes: LateinitDebugClasses? = null
  private var loop = Loop()

  private fun startDebuggingProcess(session: XDebugSession, debugPort: Int): StarlarkDebugProcess {
    val connector = StarlarkSocketConnector.tryConnectTo(
      debugPort,
      this::stop,
      Registry.intValue("bazel.starlark.debug.socket.attempts"),
      Registry.intValue("bazel.starlark.debug.socket.interval").toLong(),
    )
    val initializedClasses = StarlarkDebugMessenger(connector, session).let {
      val breakpointHandler = StarlarkBreakpointHandler(it)
      val eventHandler = ThreadAwareEventHandler(session, it, breakpointHandler::getBreakpointByPathAndLine)
      LateinitDebugClasses(
        messenger = it,
        breakpointHandler = breakpointHandler,
        eventHandler = eventHandler,
      )
    }
    classes = initializedClasses
    state = State.RUNNING
    ProgressManager.getInstance().run(loop)
    val debugProcess =
      StarlarkDebugProcess(connector, session, initializedClasses.breakpointHandler, taskListener.console, this::stop)
    taskListener.replaceSuspendChecker { debugProcess.isSuspended() }
    return debugProcess
  }

  /** This function needs to be safe to call multiple times */
  private fun stop() {
    if (state != State.DISPOSED) {
      futureToCancelOnStop?.cancel(true)
      classes?.messenger?.close()
      state = State.DISPOSED
    }
    taskListener.clearSuspendChecker()
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
    breakpointHandler: StarlarkBreakpointHandler,
    eventHandler: ThreadAwareEventHandler,
  ): () -> Unit {
    this.classes = LateinitDebugClasses(messenger, breakpointHandler, eventHandler)
    return this.loop::performEventLoopIteration
  }

  private enum class State {
    READY,
    RUNNING,
    DISPOSED,
  }

  private inner class Loop : Task.Backgroundable(project, BazelPluginBundle.message("starlark.debug.task.title")) {
    override fun run(indicator: ProgressIndicator) {
      while (state == State.RUNNING) {
        performEventLoopIteration()
      }
    }

    @VisibleForTesting
    fun performEventLoopIteration() {
      try {
        classes?.apply { messenger.readEventAndHandle(eventHandler) }
      } catch (_: Exception) {
        stop()
      }
    }
  }

  inner class ProcessStarter(private val port: Int) : XDebugProcessStarter() {
    override fun start(session: XDebugSession): XDebugProcess =
      startDebuggingProcess(session, port)
  }
}

private data class LateinitDebugClasses(
  val messenger: StarlarkDebugMessenger,
  val breakpointHandler: StarlarkBreakpointHandler,
  val eventHandler: ThreadAwareEventHandler,
)
