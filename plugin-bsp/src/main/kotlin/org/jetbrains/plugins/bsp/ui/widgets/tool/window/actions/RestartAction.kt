package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.connection.BspGeneratorConnection
import org.jetbrains.plugins.bsp.server.tasks.CollectProjectDetailsTask
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class RestartAction :
  AnAction({ BspAllTargetsWidgetBundle.message("restart.action.text") }, BspPluginIcons.restart) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doAction(project)
    } else {
      log.warn("RestartAction cannot be performed! Project not available.")
    }
  }

  private fun doAction(project: Project) {
    val bspConnectionService = BspConnectionService.getInstance(project)

    if (bspConnectionService.connection != null && bspConnectionService.connection is BspGeneratorConnection) {
      val connection = bspConnectionService.connection as BspGeneratorConnection
      val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
      bspSyncConsole.startTask("bsp-restart", "Restart", "Restarting...")

      val collectProjectDetailsTask = CollectProjectDetailsTask(project, "bsp-restart").prepareBackgroundTask()
      collectProjectDetailsTask.executeInTheBackground(
        name = "Restarting...",
        cancelable = true,
        beforeRun = { connection.restart("bsp-restart") },
        afterOnSuccess = { bspSyncConsole.finishTask("bsp-restart", "Restarting done!") }
      )
    }
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doUpdate(project, e)
    } else {
      log.warn("RestartAction cannot be updated! Project not available.")
    }
  }

  private fun doUpdate(project: Project, e: AnActionEvent) {
    val bspConnectionService = BspConnectionService.getInstance(project)
    e.presentation.isEnabled = bspConnectionService.connection?.isConnected() == true
    e.presentation.isVisible = bspConnectionService.connection is BspGeneratorConnection
  }

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.BGT

  private companion object {
    private val log = logger<RestartAction>()
  }
}
