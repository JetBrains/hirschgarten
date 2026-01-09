package org.jetbrains.bazel.logger

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

  fun message(message: String) {
    log(MessageType.LOG, message)
  }

  fun warn(message: String) {
    log(MessageType.WARNING, message)
  }

  fun publishDiagnostics(event: PublishDiagnosticsParams) {
    bspClient.onBuildPublishDiagnostics(event)
  }

  fun messageWithoutNewLine(message: String) {
    log(MessageType.LOG, message, addNewLine = false)
  }

  private fun log(messageType: MessageType, message: String, addNewLine: Boolean = true) {
    val messageWithNewLine = if (addNewLine && !message.endsWith('\n')) {
      message + '\n'
    }
    else {
      message
    }
    val params = LogMessageParams(messageType, message = messageWithNewLine, originId = originId)
    bspClient.onBuildLogMessage(params)
  }
}
