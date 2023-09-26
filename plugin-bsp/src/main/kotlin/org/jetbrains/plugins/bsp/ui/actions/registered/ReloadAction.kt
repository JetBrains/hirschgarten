package org.jetbrains.plugins.bsp.ui.actions.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.server.tasks.SyncProjectTask
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

public class ReloadAction : SuspendableAction({ BspPluginBundle.message("reload.action.text") }), DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    SyncProjectTask(project).execute(
      shouldBuildProject = false,
      shouldReloadConnection = true,
    )
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isVisible = project.isBspProject
    e.presentation.isEnabled = shouldBeEnabled(project)
  }

  private fun shouldBeEnabled(project: Project): Boolean {
    val isConnected = BspConnectionService.getInstance(project).value != null

    return project.isBspProject && isConnected && !project.isSyncInProgress()
  }

  private fun Project.isSyncInProgress() =
    BspConsoleService.getInstance(this).bspSyncConsole.hasTasksInProgress()
}
