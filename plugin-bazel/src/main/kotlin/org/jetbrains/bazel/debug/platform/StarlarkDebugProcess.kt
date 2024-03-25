package org.jetbrains.bazel.debug.platform

import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import com.intellij.xdebugger.frame.XSuspendContext
import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger
import org.jetbrains.bazel.debug.connector.StarlarkSocketConnector
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

class StarlarkDebugProcess(
  connector: StarlarkSocketConnector,
  session: XDebugSession,
  breakpointHandler: XBreakpointHandler<*>,
  private val onStop: () -> Unit,
) : XDebugProcess(session) {
  init {
    session.setPauseActionSupported(true)
  }

  private val messenger = StarlarkDebugMessenger(connector, session)
  private val breakpointHandler = arrayOf(breakpointHandler)

  override fun sessionInitialized() {
    messenger.startDebugging()
  }

  override fun startPausing() {
    messenger.pauseAllThreads()
  }

  override fun getEditorsProvider(): XDebuggerEditorsProvider = StarlarkDebuggerEditorsProvider()

  override fun getBreakpointHandlers(): Array<XBreakpointHandler<*>> = breakpointHandler

  override fun stop() {
    onStop()
  }

  override fun resume(context: XSuspendContext?) {
    messenger.resumeAllThreads()
  }

  override fun startStepInto(context: XSuspendContext?) = startStep(context, SDP.Stepping.INTO)

  override fun startStepOver(context: XSuspendContext?) = startStep(context, SDP.Stepping.OVER)

  override fun startStepOut(context: XSuspendContext?) = startStep(context, SDP.Stepping.OUT)

  private fun startStep(context: XSuspendContext?, steppingType: SDP.Stepping) {
    val topActiveFrame = context?.activeExecutionStack?.topFrame as? StarlarkStackFrame
    topActiveFrame?.let {
      messenger.resumeThread(it.threadId, steppingType)
    }
  }
}
