package org.jetbrains.bsp.testkit.client

import ch.epfl.scala.bsp4j.*
import java.util.*

class MockClient : BuildClient {
    private val showMessage = ArrayList<ShowMessageParams>()
    private val logMessage = ArrayList<LogMessageParams>()
    private val taskStart = ArrayList<TaskStartParams>()
    private val taskProgress = ArrayList<TaskProgressParams>()
    private val taskFinish = ArrayList<TaskFinishParams>()
    private val publishDiagnostics = ArrayList<PublishDiagnosticsParams>()
    private val didChangeBuildTarget = ArrayList<DidChangeBuildTarget>()

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

    override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
        publishDiagnostics.add(params)
    }

    override fun onBuildTargetDidChange(params: DidChangeBuildTarget) {
        didChangeBuildTarget.add(params)
    }
}