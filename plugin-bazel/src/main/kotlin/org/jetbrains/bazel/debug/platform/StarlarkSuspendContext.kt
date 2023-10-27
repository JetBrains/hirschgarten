package org.jetbrains.bazel.debug.platform

import com.intellij.util.containers.toArray
import com.intellij.xdebugger.frame.XExecutionStack
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import com.intellij.xdebugger.frame.XSuspendContext

class StarlarkSuspendContext(
  threads: List<SDP.PausedThread>,
  private val frameListComputer: (Long, (List<SDP.Frame>) -> Unit) -> Unit,
  private val childrenComputer: (Long, Long, (List<SDP.Value>) -> Unit) -> Unit,
  private val evaluatorProvider: StarlarkDebuggerEvaluator.Provider,
  primaryThreadId: Long? = null
) : XSuspendContext() {
  private val executionStacks = threads.map { StarlarkExecutionStack(it, frameListComputer, childrenComputer, evaluatorProvider) }
  private val activeExecutionStack = primaryThreadId?.let { primary ->
    val index = threads.indexOfFirst { it.id == primary }
    executionStacks[index]
  } ?: executionStacks.firstOrNull()



  override fun getActiveExecutionStack(): XExecutionStack? = activeExecutionStack

  override fun getExecutionStacks(): Array<XExecutionStack> = executionStacks.toArray(emptyArray())

  override fun computeExecutionStacks(container: XExecutionStackContainer?) {
    container?.addExecutionStack(executionStacks, true)
  }
}
