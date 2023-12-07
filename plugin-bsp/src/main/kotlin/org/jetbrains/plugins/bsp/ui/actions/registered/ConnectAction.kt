package org.jetbrains.plugins.bsp.ui.actions.registered

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.server.tasks.CollectProjectDetailsTask
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

public class ConnectAction : SuspendableAction({ BspPluginBundle.message("connect.action.text") }), DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val bspSyncConsole = BspConsoleService.getInstance(project).bspSyncConsole
    val collectProjectDetailsTask = CollectProjectDetailsTask(project, "bsp-connect")
    bspSyncConsole.startTask(
      taskId = "bsp-connect",
      title = BspPluginBundle.message("console.task.connect.title"),
      message = BspPluginBundle.message("console.task.connect.in.progress"),
      cancelAction = { collectProjectDetailsTask.cancelExecution() },
    )

    try {
      project.connection.connect("bsp-connect") { collectProjectDetailsTask.cancelExecution() }
      collectProjectDetailsTask.execute(
        name = "Connecting...",
        cancelable = true,
      )
      bspSyncConsole.finishTask("bsp-connect", BspPluginBundle.message("console.task.connect.success"))
    } catch (e: Exception) {
      bspSyncConsole.finishTask("bsp-connect",
        BspPluginBundle.message("console.task.connect.failed"), FailureResultImpl(e))
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabled = !project.connection.isConnected()
  }
}
