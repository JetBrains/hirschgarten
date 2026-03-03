package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

interface BazelTaskEventsHandler {
  @ApiStatus.Internal
  fun onBuildTaskStart(params: TaskStartParams)
  @ApiStatus.Internal
  fun onBuildTaskFinish(params: TaskFinishParams)

  @ApiStatus.Internal
  fun onBuildLogMessage(params: LogMessageParams)
  fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams)
  @ApiStatus.Internal
  fun onPublishCoverageReport(report: CoverageReport)
  @ApiStatus.Internal
  fun onCachedTestLog(testLog: CachedTestLog)
}

@ApiStatus.Internal
interface BazelTaskLogger {
  fun info(message: String)
  fun message(message: String)
  fun warn(message: String)
  fun error(errorMessage: String)
}

@ApiStatus.Internal
fun BazelTaskEventsHandler.asLogger(taskId: TaskId): BazelTaskLogger {
  val taskEventsHandler = this
  return object: BazelTaskLogger {
    override fun info(message: String) {
      log(MessageType.INFO, message)
    }

    override fun message(message: String) {
      log(MessageType.LOG, message)
    }

    override fun warn(message: String) {
      log(MessageType.WARNING, message)
    }

    override fun error(errorMessage: String) {
      log(MessageType.ERROR, errorMessage)
    }

    private fun log(messageType: MessageType, message: String) {
      val messageWithNewLine = if (message.endsWith('\r') || message.endsWith('\n')) {
        message
      }
      else {
        message + '\n'
      }
      val params = LogMessageParams(taskId, messageType, message = messageWithNewLine)
      taskEventsHandler.onBuildLogMessage(params)
    }
  }
}
