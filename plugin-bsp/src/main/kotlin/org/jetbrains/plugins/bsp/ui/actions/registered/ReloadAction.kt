package org.jetbrains.plugins.bsp.ui.actions.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.SyncProjectTask
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

public class ReloadAction : SuspendableAction({ BspPluginBundle.message("reload.action.text") }), DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    SyncProjectTask(project).execute(
      shouldRunInitialSync = true,
      shouldBuildProject = false,
      shouldRunResync = false,
    )
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabled = !project.isSyncInProgress()
  }

  private fun Project.isSyncInProgress() =
    BspConsoleService.getInstance(this).bspSyncConsole.hasTasksInProgress()
}
