package org.jetbrains.bazel.debug.platform

import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.debug.connector.ThreadAwareEventHandler
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

class StarlarkDebuggerEvaluator(private val threadId: Long, private val eventHandler: ThreadAwareEventHandler) : XDebuggerEvaluator() {
  override fun evaluate(expression: String, callback: XEvaluationCallback, sourcePosition: XSourcePosition?) =
    eventHandler.evaluate(threadId, expression, callback.createEventCallback(threadId))

  private fun XEvaluationCallback.createEventCallback(threadId: Long): ((SDP.Value?) -> Unit) = { value ->
    if (value != null) {
      val result = StarlarkValue.fromProto(value, threadId, eventHandler.valueComputer)
      this.evaluated(result)
    } else {
      this.errorOccurred(StarlarkBundle.message("starlark.debug.value.obtain.failed"))
    }
  }

  /** Always resolves to an error with given message */
  class ErrorEvaluator(private val message: String) : XDebuggerEvaluator() {
    override fun evaluate(expression: String, callback: XEvaluationCallback, sourcePosition: XSourcePosition?) {
      callback.errorOccurred(message)
    }
  }

  class Provider(private val eventHandler: ThreadAwareEventHandler) {
    fun obtain(threadId: Long): XDebuggerEvaluator = StarlarkDebuggerEvaluator(threadId, eventHandler)
  }
}
