package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j._

import scala.collection.mutable

class MockClient extends BuildClient {
  private val showMessage = new mutable.ListBuffer[ShowMessageParams]
  private val logMessage = new mutable.ListBuffer[LogMessageParams]
  private val taskStart = new mutable.ListBuffer[TaskStartParams]
  private val taskProgress = new mutable.ListBuffer[TaskProgressParams]
  private val taskFinish = new mutable.ListBuffer[TaskFinishParams]
  private val publishDiagnostics = new mutable.ListBuffer[PublishDiagnosticsParams]
  private val didChangeBuildTarget = new mutable.ListBuffer[DidChangeBuildTarget]

  def getShowMessageNotifications: List[ShowMessageParams] = showMessage.toList

  def getLogMessageNotifications: List[LogMessageParams] = logMessage.toList

  def getTaskStartNotifications: List[TaskStartParams] = taskStart.toList

  def getTaskProgressNotifications: List[TaskProgressParams] = taskProgress.toList

  def getTaskFinishNotifications: List[TaskFinishParams] = taskFinish.toList

  def getPublishDiagnosticsNotifications: List[PublishDiagnosticsParams] = publishDiagnostics.toList

  def getDidChangeBuildTargetNotifications: List[DidChangeBuildTarget] = didChangeBuildTarget.toList

  override def onBuildShowMessage(params: ShowMessageParams): Unit = showMessage += params

  override def onBuildLogMessage(params: LogMessageParams): Unit = logMessage += params

  override def onBuildTaskStart(params: TaskStartParams): Unit = taskStart += params

  override def onBuildTaskProgress(params: TaskProgressParams): Unit = taskProgress += params

  override def onBuildTaskFinish(params: TaskFinishParams): Unit = taskFinish += params

  override def onBuildPublishDiagnostics(params: PublishDiagnosticsParams): Unit =
    publishDiagnostics += params

  override def onBuildTargetDidChange(params: DidChangeBuildTarget): Unit =
    didChangeBuildTarget += params

  override def onRunPrintStderr(params: PrintParams): Unit = {}

  override def onRunPrintStdout(params: PrintParams): Unit = {}
}
