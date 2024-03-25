package org.jetbrains.bazel.debug.connector

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugProcessStarter
import com.intellij.xdebugger.XDebugSession
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointHandler
import org.jetbrains.bazel.debug.platform.StarlarkDebugProcess
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import kotlin.concurrent.Volatile

class StarlarkDebugManager(
  private val project: Project,
  private val port: Int,
) {
  @Volatile private var state = State.READY
  private var classes: LateinitDebugClasses? = null
  private var loop = Loop()

  private fun startDebuggingProcess(session: XDebugSession): StarlarkDebugProcess {
    val connector = StarlarkSocketConnector.connectTo(port, this::stop)
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
    return StarlarkDebugProcess(connector, session, initializedClasses.breakpointHandler, this::stop)
  }

  /** This function needs to be safe to call multiple times */
  private fun stop() {
    if (state != State.DISPOSED) {
      classes?.messenger?.close()
      state = State.DISPOSED
    }
  }

  private enum class State {
    READY,
    RUNNING,
    DISPOSED,
  }

  @Suppress("DialogTitleCapitalization")  // it triggers due to Starlark starting with a capital letter
  private inner class Loop : Task.Backgroundable(project, StarlarkBundle.message("starlark.debug.task.title")) {
    override fun run(indicator: ProgressIndicator) {
      while (state == State.RUNNING) {
        classes?.let {
          if (!it.messenger.isClosed()) {
            it.messenger.readEventAndHandle(it.eventHandler)
          }
        }
      }
    }
  }

  inner class ProcessStarter : XDebugProcessStarter() {
    override fun start(session: XDebugSession): XDebugProcess =
      startDebuggingProcess(session)
  }
}

private data class LateinitDebugClasses(
  val messenger: StarlarkDebugMessenger,
  val breakpointHandler: StarlarkBreakpointHandler,
  val eventHandler: ThreadAwareEventHandler,
)
