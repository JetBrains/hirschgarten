package org.jetbrains.bazel.debug.platform

import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger
import org.jetbrains.bazel.debug.connector.StarlarkValueComputer
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

class StarlarkDebuggerEvaluator(
  private val threadId: Long,
  private val valueComputer: StarlarkValueComputer,
  private val messenger: StarlarkDebugMessenger,
) : XDebuggerEvaluator() {
  override fun evaluate(expression: String, callback: XEvaluationCallback, sourcePosition: XSourcePosition?) {
    messenger.evaluate(threadId, expression)
      .thenAccept { it.callBack(callback) }
  }

  private fun SDP.EvaluateResponse?.callBack(callback: XEvaluationCallback) {
    this?.result
      ?.let { StarlarkValue.fromProto(it, threadId, valueComputer) }
      ?.also { callback.evaluated(it) }
      ?: callback.errorOccurred(BazelPluginBundle.message("starlark.debug.value.obtain.failed"))
  }

  /** Always resolves to an error with given message */
  class ErrorEvaluator(private val message: String) : XDebuggerEvaluator() {
    override fun evaluate(expression: String, callback: XEvaluationCallback, sourcePosition: XSourcePosition?) {
      callback.errorOccurred(message)
    }
  }

  class Provider(private val valueComputer: StarlarkValueComputer, private val messenger: StarlarkDebugMessenger) {
    fun obtain(threadId: Long): XDebuggerEvaluator =
      StarlarkDebuggerEvaluator(threadId, valueComputer, messenger)
  }
}
