package org.jetbrains.bazel.debug.platform

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import org.jetbrains.bazel.debug.connector.StarlarkValueComputer

class StarlarkExecutionStack(
  private val thread: SDP.PausedThread,
  private val valueComputer: StarlarkValueComputer,
  private val evaluatorProvider: StarlarkDebuggerEvaluator.Provider,
) : XExecutionStack("${thread.id}: ${thread.name}") {
  private var topFrame: XStackFrame? = null

  override fun getTopFrame(): XStackFrame? = topFrame

  override fun computeStackFrames(
    startIndex: Int, // index of the frame to start from (0 = top frame)
    container: XStackFrameContainer?,
  ) {
    // we don't want to answer to subsequent computeStackFrames calls, because we compute all frames at once
    if (container != null && startIndex == 0) {
       valueComputer.computeFramesForExecutionStack(thread.id) { frames ->
        val xStackFrames = frames.map { it.toStackFrame() }
        topFrame = xStackFrames.firstOrNull()?.also { it.isTopFrame = true }
        container.addStackFrames(xStackFrames, true)
      }
    }
  }

  private fun SDP.Frame.toStackFrame(): StarlarkStackFrame =
    StarlarkStackFrame(this, thread.id, valueComputer, evaluatorProvider)
}
