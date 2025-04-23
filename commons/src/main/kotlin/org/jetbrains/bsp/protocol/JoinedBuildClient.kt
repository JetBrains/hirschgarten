package org.jetbrains.bsp.protocol

interface JoinedBuildClient {
  fun onBuildLogMessage(params: LogMessageParams): Unit

  fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams): Unit

  fun onBuildTaskStart(params: TaskStartParams): Unit

  fun onBuildTaskFinish(params: TaskFinishParams): Unit

  fun onPublishCoverageReport(report: CoverageReport)
}
