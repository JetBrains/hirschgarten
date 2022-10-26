package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import org.jetbrains.magicmetamodel.MagicMetaModelDiff
import org.jetbrains.plugins.bsp.connection.BspConnectionService
import org.jetbrains.plugins.bsp.import.VeryTemporaryBspResolver
import org.jetbrains.plugins.bsp.services.BspBuildConsoleService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class ReloadAction : AnAction(BspAllTargetsWidgetBundle.message("reload.action.text")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doAction(project)
    } else {
      log.warn("ReloadAction cannot be performed! Project not available.")
    }
  }

  private fun doAction(project: Project) {
    val bspConnectionService = BspConnectionService.getInstance(project)
    val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)
    val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
    val magicMetaModelService = MagicMetaModelService.getInstance(project)

    object : Task.Backgroundable(project, "Reloading...", true) {
      private var magicMetaModelDiff: MagicMetaModelDiff? = null

      override fun run(indicator: ProgressIndicator) {
        val bspSyncConsole = BspSyncConsoleService.getInstance(project).bspSyncConsole
        bspSyncConsole.startImport("bsp-reload", "BSP: Reload", "Reloading...")
        val bspResolver =
          VeryTemporaryBspResolver(
            project.stateStore.projectBasePath,
            bspConnectionService.connection!!.server!!,
            bspSyncConsoleService.bspSyncConsole,
            bspBuildConsoleService.bspBuildConsole
          )
        val projectDetails = bspResolver.collectModel()

        magicMetaModelService.magicMetaModel.clear()
        magicMetaModelService.initializeMagicModel(projectDetails)
        val magicMetaModel = magicMetaModelService.magicMetaModel
        magicMetaModelDiff = magicMetaModel.loadDefaultTargets()
      }

      override fun onSuccess() {
        runWriteAction { magicMetaModelDiff?.applyOnWorkspaceModel() }
      }
    }.queue()
  }

  public override fun update(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doUpdate(project, e)
    } else {
      log.warn("ReloadAction cannot be updated! Project not available.")
    }
  }

  private fun doUpdate(project: Project, e: AnActionEvent) {
    val bspConnectionService = BspConnectionService.getInstance(project)
    e.presentation.isEnabled = bspConnectionService.connection?.isConnected() == true
  }

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.BGT

  private companion object {
    private val log = logger<ReloadAction>()
  }
}
