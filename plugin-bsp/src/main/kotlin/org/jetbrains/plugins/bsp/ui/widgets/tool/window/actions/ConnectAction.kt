package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.services.BspBuildConsoleService
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class ConnectAction : AnAction(BspAllTargetsWidgetBundle.message("connect.action.text")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val bspConnectionService = project.getService(BspConnectionService::class.java)
    val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
    val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)
    val magicMetaModelService = MagicMetaModelService.getInstance(project)

    runBackgroundableTask("Connect action", project) {
      bspConnectionService.reconnect(project.locationHash)
      val bspResolver =
        VeryTemporaryBspResolver(project.stateStore.projectBasePath, bspConnectionService.server!!, bspSyncConsoleService.bspSyncConsole, bspBuildConsoleService.bspBuildConsole)
      val projectDetails = bspResolver.collectModel()

      magicMetaModelService.initializeMagicModel(projectDetails)
      val magicMetaModel = magicMetaModelService.magicMetaModel
      magicMetaModel.loadDefaultTargets()
      runWriteAction {
        magicMetaModel.save()
      }
    }
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project
    val connectionService = project?.getService(BspConnectionService::class.java)
    e.presentation.isEnabled = connectionService?.isRunning() == false
  }
}
