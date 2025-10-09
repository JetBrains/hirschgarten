package org.jetbrains.bsp.protocol

interface JoinedBuildClient : BuildTaskHandler {
  fun onBuildLogMessage(params: LogMessageParams)

  fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams)

  fun onPublishCoverageReport(report: CoverageReport)

  fun onCachedTestLog(testLog: CachedTestLog)
}
