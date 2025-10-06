package org.jetbrains.bazel.debug

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.debug.utils.MockLineBreakpoint
import org.junit.jupiter.api.Test

class StarlarkDebugBreakpointHandlingTest : StarlarkDebugClientTestBase() {
  @Test
  fun `breakpoint registration`() {
    val conn = establishMockConnection()
    val breakpoint1 = MockLineBreakpoint("/a/b.c", 456)
    val breakpoint2 = MockLineBreakpoint("/x/y.z", 789)

    conn.breakpointHandler.registerBreakpoint(breakpoint1)
    conn.socket
      .readRequests()
      .single()
      .setBreakpoints.breakpointCount shouldBe 1
    conn.breakpointHandler.registerBreakpoint(breakpoint2)
    conn.socket
      .readRequests()
      .single()
      .setBreakpoints.breakpointCount shouldBe 2
    conn.breakpointHandler.unregisterBreakpoint(breakpoint1, false)
    conn.socket
      .readRequests()
      .single()
      .setBreakpoints.breakpointCount shouldBe 1
    conn.breakpointHandler.unregisterBreakpoint(breakpoint2, false)
    conn.socket
      .readRequests()
      .single()
      .setBreakpoints.breakpointCount shouldBe 0
  }

  @Test
  fun `breakpoint lookup`() {
    val breakpointHandler = establishMockConnection().breakpointHandler
    val breakpoint = MockLineBreakpoint(BREAKPOINT_PATH, BREAKPOINT_LINE)

    breakpointHandler.getBreakpointByPathAndLine(BREAKPOINT_PATH, BREAKPOINT_LINE + 1).shouldBeNull()
    breakpointHandler.registerBreakpoint(breakpoint)
    breakpointHandler.getBreakpointByPathAndLine(BREAKPOINT_PATH, BREAKPOINT_LINE + 1).shouldNotBeNull()
    breakpointHandler.unregisterBreakpoint(breakpoint, false)
    breakpointHandler.getBreakpointByPathAndLine(BREAKPOINT_PATH, BREAKPOINT_LINE + 1).shouldBeNull()
  }
}

private const val BREAKPOINT_PATH = "/some/source/file.txt"
private const val BREAKPOINT_LINE = 123
