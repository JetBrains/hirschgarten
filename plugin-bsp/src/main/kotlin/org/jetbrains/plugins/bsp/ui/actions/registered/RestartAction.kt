package org.jetbrains.plugins.bsp.ui.actions.registered

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.connection.BspGeneratorConnection
import org.jetbrains.plugins.bsp.server.tasks.CollectProjectDetailsTask
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

public class RestartAction : SuspendableAction({ BspPluginBundle.message("restart.action.text") }), DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val connection = BspConnectionService.getInstance(project).value

    if (connection is BspGeneratorConnection) {
      val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
      val collectProjectDetailsTask = CollectProjectDetailsTask(project, "bsp-restart")
      connection.restart("bsp-restart")
      bspSyncConsole.startTask(
        taskId = "bsp-restart",
        title = "Restart",
        message = "Restarting...",
        cancelAction = { collectProjectDetailsTask.cancelExecution() },
      )
      try {
        collectProjectDetailsTask.execute(name = "Restarting...", cancelable = true)
        bspSyncConsole.finishTask("bsp-restart", "Restarting done!")
      } catch (e: Exception) {
        bspSyncConsole.finishTask("bsp-restart", "Restarting failed!", FailureResultImpl(e))
      }
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    val connection = BspConnectionService.getInstance(project).value
    e.presentation.isEnabled = connection?.isConnected() == true
    e.presentation.isVisible = connection is BspGeneratorConnection
  }
}
