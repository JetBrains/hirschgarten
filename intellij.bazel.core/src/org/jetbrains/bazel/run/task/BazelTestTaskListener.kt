package org.jetbrains.bazel.run.task

import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

@ApiStatus.Internal
class BazelTestTaskListener(private val handler: ProcessHandler, private val coverageReportListener: ((Path) -> Unit)? = null) :
  BazelTaskListener {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()

  override fun onTestConsoleOutput(text: String, isError: Boolean) {
    handler.notifyTextAvailable(text.toStringWithNewline(), if (isError) ProcessOutputType.STDERR else ProcessOutputType.STDOUT)
  }

  override fun onOutputStream(taskId: TaskId?, text: String) {
    ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }

  override fun onErrorStream(taskId: TaskId?, text: String) {
    ansiEscapeDecoder.escapeText(text, ProcessOutputType.STDERR) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }

  // For compatibility with older BSP servers
  // TODO: Log messages in the correct place
  override fun onLogMessage(taskId: TaskId, message: String) {
    val messageWithNewline = message.toStringWithNewline()
    ansiEscapeDecoder.escapeText(messageWithNewline, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }

  override fun onPublishCoverageReport(coverageReport: Path) {
    coverageReportListener?.invoke(coverageReport)
  }
}

/**
 * If a system message sent to the process handler does not end with a newline,
 * it might connect to another message and not be parsed correctly
 * */
private fun String.toStringWithNewline(): String = if (this.endsWith("\n")) this else "$this\n"
