package org.jetbrains.bazel.debug.console

import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.TaskId

internal class StarlarkDebugTaskListener(private val console: ConsoleView, private val usePty: Boolean) : BazelTaskListener {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()

  /** Function for checking if debug execution is currently suspended */
  private var debugPausedChecker: (() -> Boolean)? = null

  fun setDebugPausedChecker(newDebugPausedChecker: () -> Boolean) {
    debugPausedChecker = newDebugPausedChecker
  }

  override fun onLogMessage(taskId: TaskId, message: String) {
    // bazelbsp server erroneously sends a redundant log message in a loop when the debugging session is suspended
    if (debugPausedChecker?.invoke() != true) printMessage(message)
  }

  private fun printMessage(message: String) {
    if (usePty) {
      console.print(message, ConsoleViewContentType.NORMAL_OUTPUT)
    }
    else {
      ansiEscapeDecoder.escapeText(message, ProcessOutputType.STDOUT) { text, key ->
        console.print(text, ConsoleViewContentType.getConsoleViewType(key))
      }
    }
  }
}
