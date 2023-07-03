package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.tasks.CollectProjectDetailsTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class ConnectAction : AnAction(BspAllTargetsWidgetBundle.message("connect.action.text")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      BspCoroutineService.getInstance(project).start { doAction(project) }
    } else {
      log.warn("ConnectAction cannot be performed! Project not available.")
    }
  }

  private suspend fun doAction(project: Project) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    val collectProjectDetailsTask = CollectProjectDetailsTask(project, "bsp-connect")
    bspSyncConsole.startTask(
      taskId = "bsp-connect",
      title = "Connect",
      message = "Connecting...",
      cancelAction = { collectProjectDetailsTask.cancelExecution() },
    )

    try {
      BspConnectionService.getInstance(project).value!!.connect("bsp-connect")
      collectProjectDetailsTask.execute(
        name = "Connecting...",
        cancelable = true
      )
      bspSyncConsole.finishTask("bsp-connect", "Connect done!")
    } catch (e: Exception) {
      bspSyncConsole.finishTask("bsp-connect", "Connect failed!", FailureResultImpl(e))
    }
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doUpdate(project, e)
    } else {
      log.warn("ConnectAction cannot be updated! Project not available.")
    }
  }

  private fun doUpdate(project: Project, e: AnActionEvent) {
    val bspConnection = BspConnectionService.getInstance(project).value
    e.presentation.isEnabled = bspConnection?.isConnected() == false
  }

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.BGT

  private companion object {
    private val log = logger<ConnectAction>()
  }
}
