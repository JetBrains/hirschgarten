package org.jetbrains.bazel.debug.utils

import com.intellij.openapi.util.Key
import com.intellij.pom.Navigatable
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.SuspendPolicy
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointProperties

class MockLineBreakpoint(private val absPath: String, private val line: Int) : XLineBreakpoint<StarlarkBreakpointProperties> {
  override fun <T : Any?> getUserData(key: Key<T>): T? {
    unavailableInMock()
  }

  override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
    unavailableInMock()
  }

  override fun isEnabled(): Boolean {
    unavailableInMock()
  }

  override fun setEnabled(enabled: Boolean) {
    unavailableInMock()
  }

  override fun getType(): XLineBreakpointType<StarlarkBreakpointProperties> {
    unavailableInMock()
  }

  override fun getProperties(): StarlarkBreakpointProperties {
    unavailableInMock()
  }

  override fun getSourcePosition(): XSourcePosition? {
    unavailableInMock()
  }

  override fun getNavigatable(): Navigatable? {
    unavailableInMock()
  }

  override fun getSuspendPolicy(): SuspendPolicy {
    unavailableInMock()
  }

  override fun setSuspendPolicy(policy: SuspendPolicy) {
    unavailableInMock()
  }

  override fun isLogMessage(): Boolean {
    unavailableInMock()
  }

  override fun setLogMessage(logMessage: Boolean) {
    unavailableInMock()
  }

  override fun isLogStack(): Boolean {
    unavailableInMock()
  }

  override fun setLogStack(logStack: Boolean) {
    unavailableInMock()
  }

  override fun setLogExpression(expression: String?) {
    unavailableInMock()
  }

  override fun getLogExpressionObject(): XExpression? {
    unavailableInMock()
  }

  override fun setLogExpressionObject(expression: XExpression?) {
    unavailableInMock()
  }

  override fun setCondition(condition: String?) {
    unavailableInMock()
  }

  override fun getConditionExpression(): XExpression? {
    unavailableInMock()
  }

  override fun setConditionExpression(condition: XExpression?) {
    unavailableInMock()
  }

  override fun getTimeStamp(): Long {
    unavailableInMock()
  }

  override fun getLine(): Int = line

  override fun getFileUrl(): String = "file://$absPath"

  override fun getPresentableFilePath(): String {
    unavailableInMock()
  }

  override fun getShortFilePath(): String {
    unavailableInMock()
  }

  override fun isTemporary(): Boolean {
    unavailableInMock()
  }

  override fun setTemporary(temporary: Boolean) {
    unavailableInMock()
  }
}

private fun unavailableInMock(): Nothing = throw NotImplementedError("Is not implemented in this mock")
