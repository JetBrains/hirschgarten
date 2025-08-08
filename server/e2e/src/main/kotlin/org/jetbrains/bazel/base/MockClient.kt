package org.jetbrains.bazel.base

import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskStartParams

open class MockClient : JoinedBuildClient {
  private val logMessage = ArrayList<LogMessageParams>()
  private val taskStart = ArrayList<TaskStartParams>()
  private val taskFinish = ArrayList<TaskFinishParams>()
  private val publishDiagnostics = ArrayList<PublishDiagnosticsParams>()

  val publishDiagnosticsNotifications: List<PublishDiagnosticsParams>
    get() = publishDiagnostics

  fun clearDiagnostics() {
    publishDiagnostics.clear()
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    logMessage.add(params)
  }

  override fun onBuildTaskStart(params: TaskStartParams) {
    taskStart.add(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams) {
    taskFinish.add(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    publishDiagnostics.add(params)
  }

  override fun onPublishCoverageReport(report: CoverageReport) {}
}
