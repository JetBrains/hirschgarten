package org.jetbrains.bazel.run.task

import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.TaskId

open class BazelRunTaskListener(protected val handler: BazelProcessHandler) : BazelTaskListener {
  protected val ansiEscapeDecoder = AnsiEscapeDecoder()

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

  // For compatibility with older BSP servers and JB test runner
  // TODO: Log messages in the correct place
  override fun onLogMessage(message: String) {
    val messageWithNewline = if (message.endsWith("\n")) message else "$message\n"
    ansiEscapeDecoder.escapeText(messageWithNewline, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }
}
