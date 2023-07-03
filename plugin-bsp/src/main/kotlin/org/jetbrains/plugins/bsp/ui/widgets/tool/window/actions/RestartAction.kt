package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.connection.BspGeneratorConnection
import org.jetbrains.plugins.bsp.server.tasks.CollectProjectDetailsTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class RestartAction :
  AnAction({ BspAllTargetsWidgetBundle.message("restart.action.text") }, BspPluginIcons.restart) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      BspCoroutineService.getInstance(project).start { doAction(project) }
    } else {
      log.warn("RestartAction cannot be performed! Project not available.")
    }
  }

  private suspend fun doAction(project: Project) {
    val connection = BspConnectionService.getInstance(project).value

    if (connection is BspGeneratorConnection) {
      val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
      val collectProjectDetailsTask = CollectProjectDetailsTask(project, "bsp-restart")
      connection.restart("bsp-restart")
      bspSyncConsole.startTask(
        taskId = "bsp-restart",
        title = "Restart",
        message = "Restarting...",
        cancelAction = { collectProjectDetailsTask.cancelExecution() }
      )
      try {
        collectProjectDetailsTask.execute(name = "Restarting...", cancelable = true)
        bspSyncConsole.finishTask("bsp-restart", "Restarting done!")
      } catch (e: Exception) {
        bspSyncConsole.finishTask("bsp-restart", "Restarting failed!", FailureResultImpl(e))
      }
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
    val connection = BspConnectionService.getInstance(project).value
    e.presentation.isEnabled = connection?.isConnected() == true
    e.presentation.isVisible = connection is BspGeneratorConnection
  }

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.BGT

  private companion object {
    private val log = logger<RestartAction>()
  }
}
