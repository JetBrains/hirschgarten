package org.jetbrains.bazel.debug.console

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.services.BspTaskListener

class StarlarkDebugTaskListener(
  project: Project,
) : BspTaskListener {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()
  val console: ConsoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console

  private var suspendChecker: (() -> Boolean)? = null

  override fun onLogMessage(message: String) {
    // bazelbsp server erroneously sends a redundant log message in a loop when the debugging session is suspended
    if (suspendChecker?.invoke() != true) printMessage(message)
  }

  private fun printMessage(message: String) {
    ansiEscapeDecoder.escapeText(message, ProcessOutputType.STDOUT) { text, key ->
      console.print(text, ConsoleViewContentType.getConsoleViewType(key))
    }
    console.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
  }

  fun replaceSuspendChecker(newSuspendChecker: () -> Boolean) {
    suspendChecker = newSuspendChecker
  }

  fun clearSuspendChecker() {
    suspendChecker = null
  }
}
