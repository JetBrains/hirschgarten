package org.jetbrains.bazel.debug.connector

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.debug.console.StarlarkDebugTaskListener
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointHandler
import org.jetbrains.bazel.debug.platform.StarlarkDebugProcess
import java.io.IOException
import java.util.concurrent.CancellationException

/**
 * Main controller for everything related to Starlark debug.
 * All debug elements which require disposal should have this class registered as their disposable parent.
 * Must be disposed after use.
 */
class StarlarkDebugSessionManager(private val project: Project) : Disposable {
  val taskListener: StarlarkDebugTaskListener

  private val console: ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

  private var jobToCancel: Job? = null // job to be cancelled when debug stops
  private var messenger: StarlarkDebugMessenger? = null
  private var eventHandler: ThreadAwareEventHandler? = null

  private var loop: Job? = null

  init {
    Disposer.register(this, console)
    taskListener = StarlarkDebugTaskListener(console)
  }

  private fun startDebuggingProcess(session: XDebugSession, connector: StarlarkSocketConnector): StarlarkDebugProcess =
    StarlarkDebugMessenger(connector, session).let {
      messenger = it
      val breakpointHandler = StarlarkBreakpointHandler(it)
      eventHandler = ThreadAwareEventHandler(session, it, breakpointHandler::getBreakpointByPathAndLine)
      loop = BazelCoroutineService.getInstance(project).start { runLoop() }
      val debugProcess =
        StarlarkDebugProcess(connector, session, breakpointHandler, console)
      taskListener.setDebugPausedChecker { debugProcess.isSuspended() }
      return@let debugProcess
    }

  private suspend fun runLoop() =
    withBackgroundProgress(project, BazelPluginBundle.message("starlark.debug.task.title"), true) {
      while (this.isActive) {
        performEventLoopIteration()
      }
    }

  private fun performEventLoopIteration() {
    try {
      eventHandler?.let {
        messenger?.readEventAndHandle(it)
      }
    } catch (e: Exception) {
      if (e !is IOException && e !is CancellationException) {
        log.error(e) // other exceptions are not expected
      }
      stop()
    }
  }

  /** This function needs to be safe to call multiple times */
  private fun stop() {
    loop?.cancel()
    jobToCancel?.cancel()
    messenger?.close()
  }

  fun registerJobToCancel(job: Job) {
    // if for some reason, another job is registered, cancel the previous one
    jobToCancel?.cancel()
    jobToCancel = job
  }

  /** Gets a single debug session manager loop iteration for testing it without performing full session setup */
  @TestOnly
  fun getSingleLoopIteration(messenger: StarlarkDebugMessenger, eventHandler: ThreadAwareEventHandler): () -> Unit {
    this.messenger = messenger
    this.eventHandler = eventHandler
    return this::performEventLoopIteration
  }

  override fun dispose() {
    // no action - manager does not dispose anything itself, it just serves as parent for other disposables
  }

  inner class ProcessStarter(private val connector: StarlarkSocketConnector) : XDebugProcessStarter() {
    override fun start(session: XDebugSession): XDebugProcess = startDebuggingProcess(session, connector)
  }

  private companion object {
    val log = logger<StarlarkDebugSessionManager>()
  }
}
