package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runModalTask
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class DisconnectAction : AnAction(BspAllTargetsWidgetBundle.message("dis-connect.action.text")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doAction(project)
    } else {
      log.warn("DisconnectAction cannot be performed! Project not available.")
    }
  }

  private fun doAction(project: Project) {
    val bspConnectionService = BspConnectionService.getInstance(project)

    runModalTask("Disconnecting...", project = project, cancellable = false) {
      bspConnectionService.connection?.disconnect()
    }
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doUpdate(project, e)
    } else {
      log.warn("DisconnectAction cannot be updated! Project not available.")
    }
  }

  private fun doUpdate(project: Project, e: AnActionEvent) {
    val bspConnectionService = BspConnectionService.getInstance(project)
    e.presentation.isEnabled = bspConnectionService.connection?.isConnected() == true
  }

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.BGT

  private companion object {
    private val log = logger<DisconnectAction>()
  }
}
