package org.jetbrains.bsp.testkit.client

import org.jetbrains.bsp.protocol.DidChangeBuildTarget
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.PrintParams
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.PublishOutputParams
import org.jetbrains.bsp.protocol.ShowMessageParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskProgressParams
import org.jetbrains.bsp.protocol.TaskStartParams

open class MockClient : JoinedBuildClient {
  private val showMessage = ArrayList<ShowMessageParams>()
  private val logMessage = ArrayList<LogMessageParams>()
  private val taskStart = ArrayList<TaskStartParams>()
  private val taskProgress = ArrayList<TaskProgressParams>()
  private val taskFinish = ArrayList<TaskFinishParams>()
  private val publishDiagnostics = ArrayList<PublishDiagnosticsParams>()
  private val didChangeBuildTarget = ArrayList<DidChangeBuildTarget>()
  private val printStdout = ArrayList<PrintParams>()
  private val printStderr = ArrayList<PrintParams>()

  val showMessageNotifications: List<ShowMessageParams>
    get() = showMessage

  val logMessageNotifications: List<LogMessageParams>
    get() = logMessage

  val taskStartNotifications: List<TaskStartParams>
    get() = taskStart

  val taskProgressNotifications: List<TaskProgressParams>
    get() = taskProgress

  val taskFinishNotifications: List<TaskFinishParams>
    get() = taskFinish

  val publishDiagnosticsNotifications: List<PublishDiagnosticsParams>
    get() = publishDiagnostics

  val didChangeBuildTargetNotifications: List<DidChangeBuildTarget>
    get() = didChangeBuildTarget

  fun clearDiagnostics() {
    publishDiagnostics.clear()
  }

  override fun onBuildShowMessage(params: ShowMessageParams) {
    showMessage.add(params)
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    logMessage.add(params)
  }

  override fun onBuildTaskStart(params: TaskStartParams) {
    taskStart.add(params)
  }

  override fun onBuildTaskProgress(params: TaskProgressParams) {
    taskProgress.add(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams) {
    taskFinish.add(params)
  }

  override fun onRunPrintStdout(params: PrintParams) {
    printStdout.add(params)
  }

  override fun onRunPrintStderr(params: PrintParams) {
    printStderr.add(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    publishDiagnostics.add(params)
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget) {
    didChangeBuildTarget.add(params)
  }

  override fun onBuildPublishOutput(params: PublishOutputParams) {}
}
