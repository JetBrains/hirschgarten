package org.jetbrains.bsp.protocol

interface BazelTaskEventsHandler {
  fun onBuildTaskStart(params: TaskStartParams)
  fun onBuildTaskFinish(params: TaskFinishParams)

  fun onBuildLogMessage(params: LogMessageParams)
  fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams)
  fun onPublishCoverageReport(report: CoverageReport)
  fun onCachedTestLog(testLog: CachedTestLog)
}

interface BazelTaskLogger {
  fun message(message: String)
  fun warn(message: String)
  fun error(errorMessage: String)
}

fun BazelTaskEventsHandler.asLogger(taskId: TaskId): BazelTaskLogger {
  val taskEventsHandler = this
  return object: BazelTaskLogger {
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
