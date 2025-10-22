package org.jetbrains.bazel.debug

import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import org.jetbrains.bazel.debug.platform.StarlarkBreakpointHandler.Companion.absolutePath
import org.jetbrains.bazel.debug.utils.MockLineBreakpoint
import org.jetbrains.bazel.workspace.model.matchers.shouldBeEqual
import org.junit.jupiter.api.Test
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

class StarlarkDebugEventHandlingTest : StarlarkDebugClientTestBase() {
  @Test
  fun `debug error handling`() {
    val conn = establishMockConnection()
    val errorMessage = "An error"
    val error =
      SDP.Error
        .newBuilder()
        .setMessage(errorMessage)
        .build()
    val event =
      SDP.DebugEvent
        .newBuilder()
        .setError(error)
        .build()
    conn.socket.sendMockEvent(event)

    conn.session.lastError.shouldBeNull()
    conn.messenger.readEventAndHandle(conn.eventHandler)
    conn.session.lastError.let {
      it.shouldNotBeNull()
      it shouldBeEqual errorMessage
    }
  }

  @Test
  fun `enabled breakpoint event handling`() {
    val conn = establishMockConnection()
    conn.registerExampleBreakpoint()
    createPauseEvent(SDP.PauseReason.HIT_BREAKPOINT).sendEventAndReact(conn)

    (conn.session.breakpointReached as XLineBreakpoint<*>).let {
      it.line shouldBeEqual BREAKPOINT_LINE
      it.absolutePath() shouldBeEqual BREAKPOINT_PATH
    }
    conn.socket
      .readRequests()
      .any { it.payloadCase == SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION }
      .shouldBeFalse()
  }

  @Test
  fun `disabled breakpoint event handling`() {
    val conn = establishMockConnection()
    conn.registerExampleBreakpoint()
    conn.session.ignoreBreakpoints = true
    createPauseEvent(SDP.PauseReason.HIT_BREAKPOINT).sendEventAndReact(conn)

    conn.session.breakpointReached.shouldNotBeNull()
    conn.socket
      .readRequests()
      .any { it.payloadCase == SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION }
      .shouldBeTrue()
  }

  @Test
  fun `initial execution pause handling`() {
    val conn = establishMockConnection()
    createPauseEvent(SDP.PauseReason.INITIALIZING).sendEventAndReact(conn)

    conn.session.breakpointReached.shouldBeNull()
    conn.socket
      .readRequests()
      .any { it.payloadCase == SDP.DebugRequest.PayloadCase.CONTINUE_EXECUTION }
      .shouldBeFalse()
  }

  private fun MockConnectionPack.registerExampleBreakpoint() {
    val breakpoint = MockLineBreakpoint(BREAKPOINT_PATH, BREAKPOINT_LINE)
    breakpointHandler.registerBreakpoint(breakpoint)
    socket.clearBuffers()
  }

  private fun SDP.DebugEvent.sendEventAndReact(conn: MockConnectionPack) {
    conn.socket.sendMockEvent(this)
    conn.messenger.readEventAndHandle(conn.eventHandler)
  }
}

private const val BREAKPOINT_PATH = "/some/source/file.txt"
private const val BREAKPOINT_LINE = 123

private const val THREAD_NAME = "A thread"
private const val THREAD_ID = 12345L

private fun createPauseEvent(reason: SDP.PauseReason): SDP.DebugEvent {
  val location =
    SDP.Location
      .newBuilder()
      .setPath(BREAKPOINT_PATH)
      .setLineNumber(BREAKPOINT_LINE + 1) // Bazel uses 1-indexed lines
      .build()
  val thread =
    SDP.PausedThread
      .newBuilder()
      .setName(THREAD_NAME)
      .setId(THREAD_ID)
      .setPauseReason(reason)
      .setLocation(location)
      .build()
  val pause =
    SDP.ThreadPausedEvent
      .newBuilder()
      .setThread(thread)
      .build()
  return SDP.DebugEvent
    .newBuilder()
    .setThreadPaused(pause)
    .build()
}
