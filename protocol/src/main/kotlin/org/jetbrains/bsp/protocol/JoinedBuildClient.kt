package org.jetbrains.bsp.protocol

interface JoinedBuildClient {
  fun onBuildShowMessage(params: ShowMessageParams): Unit

  fun onBuildLogMessage(params: LogMessageParams): Unit

  fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams): Unit

  fun onBuildTargetDidChange(params: DidChangeBuildTarget): Unit

  fun onBuildTaskStart(params: TaskStartParams): Unit

  fun onBuildTaskProgress(params: TaskProgressParams): Unit

  fun onBuildTaskFinish(params: TaskFinishParams): Unit

  fun onRunPrintStdout(params: PrintParams): Unit

  fun onRunPrintStderr(params: PrintParams): Unit

  fun onPublishCoverageReport(report: CoverageReport)
}
