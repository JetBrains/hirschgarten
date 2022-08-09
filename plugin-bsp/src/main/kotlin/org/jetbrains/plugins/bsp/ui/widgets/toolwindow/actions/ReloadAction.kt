package org.jetbrains.plugins.bsp.ui.widgets.toolwindow.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver
import org.jetbrains.plugins.bsp.ui.widgets.toolwindow.all.targets.BspAllTargetsWidgetBundle

public class ReloadAction : AnAction(BspAllTargetsWidgetBundle.message("reload.action.text")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val connectionService = project.getService(BspConnectionService::class.java)
    val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)

    val bspResolver =
      VeryTemporaryBspResolver(project.stateStore.projectBasePath, connectionService.server!!, bspSyncConsoleService.bspSyncConsole)

    bspResolver.collectModel()
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project
    val connectionService = project?.getService(BspConnectionService::class.java)
    e.presentation.isEnabled = connectionService?.isRunning() == true
  }
}
