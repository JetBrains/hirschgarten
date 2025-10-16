package org.jetbrains.bazel.debug.utils

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XSuspendContext
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler
import com.intellij.xdebugger.stepping.XSmartStepIntoVariant
import org.jetbrains.bazel.sdkcompat.XDebugSessionCompat
import javax.swing.Icon
import javax.swing.event.HyperlinkListener

class MockDebugSession : XDebugSessionCompat() {
  var ignoreBreakpoints: Boolean = false
  var breakpointReached: XBreakpoint<*>? = null
    private set
  var lastError: String? = null

  override fun isMixedModeCompat(): Boolean = true

  override fun isStopped(): Boolean {
    unavailableInMock()
  }

  override fun isPaused(): Boolean {
    unavailableInMock()
  }

  override fun getProject(): Project {
    unavailableInMock()
  }

  override fun getDebugProcess(): XDebugProcess {
    unavailableInMock()
  }

  override fun isSuspended(): Boolean {
    unavailableInMock()
  }

  override fun getCurrentStackFrame(): XStackFrame? {
    unavailableInMock()
  }

  override fun getSuspendContext(): XSuspendContext {
    unavailableInMock()
  }

  override fun getCurrentPosition(): XSourcePosition? {
    unavailableInMock()
  }

  override fun getTopFramePosition(): XSourcePosition? {
    unavailableInMock()
  }

  override fun stepOver(ignoreBreakpoints: Boolean) {
    // no operation
  }

  override fun stepInto() {
    // no operation
  }

  override fun stepOut() {
    // no operation
  }

  override fun forceStepInto() {
    // no operation
  }

  override fun runToPosition(position: XSourcePosition, ignoreBreakpoints: Boolean) {
    // no operation
  }

  override fun pause() {
    // no operation
  }

  override fun resume() {
    // no operation
  }

  override fun showExecutionPoint() {
    // no operation
  }

  override fun setCurrentStackFrame(
    executionStack: XExecutionStack,
    frame: XStackFrame,
    isTopFrame: Boolean,
  ) {
    // no operation
  }

  override fun updateBreakpointPresentation(
    breakpoint: XLineBreakpoint<*>,
    icon: Icon?,
    errorMessage: String?,
  ) {
    // no operation
  }

  override fun setBreakpointVerified(breakpoint: XLineBreakpoint<*>) {
    // no operation
  }

  override fun setBreakpointInvalid(breakpoint: XLineBreakpoint<*>, errorMessage: String?) {
    // no operation
  }

  override fun breakpointReached(
    breakpoint: XBreakpoint<*>,
    evaluatedLogExpression: String?,
    suspendContext: XSuspendContext,
  ): Boolean {
    breakpointReached = breakpoint
    return !ignoreBreakpoints
  }

  override fun positionReached(suspendContext: XSuspendContext) {
    // no operation
  }

  override fun sessionResumed() {
    // no operation
  }

  override fun stop() {
    // no operation
  }

  override fun setBreakpointMuted(muted: Boolean) {
    // no operation
  }

  override fun areBreakpointsMuted(): Boolean {
    unavailableInMock()
  }

  override fun addSessionListener(listener: XDebugSessionListener, parentDisposable: Disposable) {
    // no operation
  }

  override fun addSessionListener(listener: XDebugSessionListener) {
    // no operation
  }

  override fun removeSessionListener(listener: XDebugSessionListener) {
    // no operation
  }

  override fun reportError(message: String) {
    lastError = message
  }

  override fun reportMessage(message: String, type: MessageType) {
    // no operation
  }

  override fun reportMessage(
    message: String,
    type: MessageType,
    listener: HyperlinkListener?,
  ) {
    // no operation
  }

  override fun getSessionName(): String {
    unavailableInMock()
  }

  override fun getRunContentDescriptor(): RunContentDescriptor {
    unavailableInMock()
  }

  override fun getRunProfile(): RunProfile? {
    unavailableInMock()
  }

  override fun setPauseActionSupported(isSupported: Boolean) {
    // no operation
  }

  override fun rebuildViews() {
    // no operation
  }

  override fun <V : XSmartStepIntoVariant?> smartStepInto(handler: XSmartStepIntoHandler<V>?, variant: V) {
    // no operation
  }

  override fun updateExecutionPosition() {
    // no operation
  }

  override fun initBreakpoints() {
    // no operation
  }

  override fun getConsoleView(): ConsoleView {
    unavailableInMock()
  }

  override fun getUI(): RunnerLayoutUi {
    unavailableInMock()
  }
}

private fun unavailableInMock(): Nothing = throw NotImplementedError("Is not implemented in this mock")
