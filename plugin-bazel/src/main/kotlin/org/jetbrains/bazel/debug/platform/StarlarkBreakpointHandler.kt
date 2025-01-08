package org.jetbrains.bazel.debug.platform

import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger

class StarlarkBreakpointHandler(private val messenger: StarlarkDebugMessenger) :
  XBreakpointHandler<StarlarkBreakpoint>(StarlarkBreakpointType::class.java) {
  private val current = mutableSetOf<StarlarkBreakpoint>()

  override fun registerBreakpoint(breakpoint: StarlarkBreakpoint) {
    synchronized(this) {
      current.add(breakpoint)
      flushBreakpoints()
    }
  }

  override fun unregisterBreakpoint(breakpoint: StarlarkBreakpoint, temporary: Boolean) {
    synchronized(this) {
      current.remove(breakpoint)
      flushBreakpoints()
    }
  }

  private fun flushBreakpoints() {
    messenger.setBreakpoints(current.toList())
  }

  fun getBreakpointByPathAndLine(path: String, line: Int): StarlarkBreakpoint? =
    current.firstOrNull {
      it.absolutePath() == path && it.line == line - 1 // turning 1-indexed back to 0-indexed
    }

  companion object {
    fun XLineBreakpoint<*>.absolutePath(): String = fileUrl.replace("file://", "")
  }
}

private typealias StarlarkBreakpoint = XLineBreakpoint<StarlarkBreakpointProperties>
