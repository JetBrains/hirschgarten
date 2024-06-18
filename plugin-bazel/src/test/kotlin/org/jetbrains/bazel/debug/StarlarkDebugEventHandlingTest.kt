package org.jetbrains.bazel.debug

import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.debug.error.StarlarkDebuggerError
import org.jetbrains.bazel.debug.utils.MockLineBreakpoint
import org.junit.jupiter.api.Test

class StarlarkDebugEventHandlingTest : StarlarkDebugClientTestBase() {
  @Test
  fun `debug error handling`() {
    val error = SDP.Error.newBuilder().setMessage("An error").build()
    val event = SDP.DebugEvent.newBuilder().setError(error).build()
    socket.sendMockEvent(event)

    shouldThrow<StarlarkDebuggerError> {
      messenger.readEventAndHandle(eventHandler)
    }
  }

  @Test
  fun `enabled breakpoint event handling`() {
    registerExampleBreakpoint()
    createPauseEvent(SDP.PauseReason.HIT_BREAKPOINT).sendEventAndReact()

    (session.breakpointReached as XLineBreakpoint<*>).let {
      it.line shouldBeEqual BREAKPOINT_LINE
      it.presentableFilePath shouldBeEqual BREAKPOINT_PATH
    }
    socket.readRequests()
      .any { it.payloadCase == SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION }
      .shouldBeFalse()
  }

  @Test
  fun `disabled breakpoint event handling`() {
    registerExampleBreakpoint()
    session.ignoreBreakpoints = true
    createPauseEvent(SDP.PauseReason.HIT_BREAKPOINT).sendEventAndReact()

    session.breakpointReached.shouldNotBeNull()
    socket.readRequests()
      .any { it.payloadCase == SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION }
      .shouldBeTrue()
  }

  @Test
  fun `initial execution pause handling`() {
    createPauseEvent(SDP.PauseReason.INITIALIZING).sendEventAndReact()

    session.breakpointReached.shouldBeNull()
    socket.readRequests()
      .any { it.payloadCase == SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION }
      .shouldBeFalse()
  }

  private fun registerExampleBreakpoint() {
    val breakpoint = MockLineBreakpoint(BREAKPOINT_PATH, BREAKPOINT_LINE)
    breakpointHandler.registerBreakpoint(breakpoint)
    socket.clearBuffers()
  }

  private fun SDP.DebugEvent.sendEventAndReact() {
    socket.sendMockEvent(this)
    messenger.readEventAndHandle(eventHandler)
  }
}

private const val BREAKPOINT_PATH = "/some/source/file.txt"
private const val BREAKPOINT_LINE = 123

private const val THREAD_NAME = "A thread"
private const val THREAD_ID = 12345L

private fun createPauseEvent(reason: SDP.PauseReason): SDP.DebugEvent {
  val location = SDP.Location.newBuilder()
    .setPath(BREAKPOINT_PATH)
    .setLineNumber(BREAKPOINT_LINE + 1)  // Bazel uses 1-indexed lines
    .build()
  val thread = SDP.PausedThread.newBuilder()
    .setName(THREAD_NAME)
    .setId(THREAD_ID)
    .setPauseReason(reason)
    .setLocation(location)
    .build()
  val pause = SDP.ThreadPausedEvent.newBuilder().setThread(thread).build()
  return SDP.DebugEvent.newBuilder().setThreadPaused(pause).build()
}
