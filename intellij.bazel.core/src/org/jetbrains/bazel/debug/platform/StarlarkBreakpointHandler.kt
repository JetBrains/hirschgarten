package org.jetbrains.bazel.debug.platform

import com.intellij.xdebugger.breakpoints.XBreakpointHandler
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelHost
import org.jetbrains.bazel.debug.connector.StarlarkDebugMessenger
import java.nio.file.InvalidPathException
import java.nio.file.Path

@ApiStatus.Internal
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
    /**
     * The path of the file of the breakpoint in the form that Bazel uses.
     * Bazel can run in another environment than the IDE, for example in WSL.
     * Then the IDE sees the file as `//wsl.localhost/Ubuntu/home/user/BUILD`, and Bazel sees it as `/home/user/BUILD`.
     */
    fun XLineBreakpoint<*>.absolutePath(): String {
      val idePath = fileUrl.replace("file://", "")
      val nioPath = try {
        Path.of(idePath)
      }
      catch (_: InvalidPathException) {
        return idePath
      }
      val host = BazelHost.of(nioPath)
      return if (host.isLocal) idePath else host.toHostPath(nioPath)
    }
  }
}

private typealias StarlarkBreakpoint = XLineBreakpoint<StarlarkBreakpointProperties>
