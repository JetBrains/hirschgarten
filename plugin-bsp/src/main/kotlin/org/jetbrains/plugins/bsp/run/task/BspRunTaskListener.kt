package org.jetbrains.plugins.bsp.run.task

import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.taskEvents.BspTaskListener
import org.jetbrains.plugins.bsp.taskEvents.TaskId

class BspRunTaskListener(private val handler: BspProcessHandler) : BspTaskListener {
  private val ansiEscapeDecoder = AnsiEscapeDecoder()

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
  override fun onLogMessage(message: String) {
    val messageWithNewline = if (message.endsWith("\n")) message else "$message\n"
    ansiEscapeDecoder.escapeText(messageWithNewline, ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }
}
