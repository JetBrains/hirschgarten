package org.jetbrains.bazel.debug.platform

import com.intellij.util.containers.toArray
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XSuspendContext
import org.jetbrains.bazel.debug.connector.StarlarkValueComputer
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

class StarlarkSuspendContext(
  threads: List<SDP.PausedThread>,
  private val valueComputer: StarlarkValueComputer,
  private val evaluatorProvider: StarlarkDebuggerEvaluator.Provider,
  primaryThreadId: Long? = null,
) : XSuspendContext() {
  private val executionStacks = threads.map { StarlarkExecutionStack(it, valueComputer, evaluatorProvider) }
  private val activeExecutionStack =
    primaryThreadId?.let { primary ->
      val index = threads.indexOfFirst { it.id == primary }
      executionStacks[index]
    } ?: executionStacks.firstOrNull()

  override fun getActiveExecutionStack(): XExecutionStack? = activeExecutionStack

  override fun getExecutionStacks(): Array<XExecutionStack> = executionStacks.toArray(emptyArray())

  override fun computeExecutionStacks(container: XExecutionStackContainer?) {
    container?.addExecutionStack(executionStacks, true)
  }
}
