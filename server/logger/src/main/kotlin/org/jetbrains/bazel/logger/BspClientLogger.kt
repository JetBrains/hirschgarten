package org.jetbrains.bazel.logger

import org.jetbrains.bazel.commons.Format
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.MessageType
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import java.time.Duration

private val LOG_OPERATION_THRESHOLD: Duration = Duration.ofMillis(100)

data class BspClientLogger(private val bspClient: JoinedBuildClient, private val originId: String? = null) {
  fun error(errorMessage: String) {
    log(MessageType.ERROR, errorMessage)
  }

  fun message(format: String, vararg args: Any?) {
    message(String.format(format, *args))
  }

  fun message(message: String) {
    log(MessageType.LOG, message)
  }

  fun warn(format: String, vararg args: Any?) {
    warn(String.format(format, *args))
  }

  fun warn(message: String) {
    log(MessageType.WARNING, message)
  }

  fun publishDiagnostics(event: PublishDiagnosticsParams) {
    bspClient.onBuildPublishDiagnostics(event)
  }

  private fun log(messageType: MessageType, message: String) {
    if (message.trim { it <= ' ' }.isNotEmpty()) {
      val params = LogMessageParams(messageType, message = message, originId = originId)
      bspClient.onBuildLogMessage(params)
    }
  }

  fun logDuration(description: String, duration: Duration) {
    if (duration >= LOG_OPERATION_THRESHOLD) {
      message("Task '%s' completed in %s", description, Format.duration(duration))
    }
  }
}
