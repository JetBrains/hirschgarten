package org.jetbrains.plugins.bsp.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.sync.scope.SecondPhaseSync
import org.jetbrains.plugins.bsp.sync.status.isSyncInProgress
import org.jetbrains.plugins.bsp.sync.task.ProjectSyncTask
import org.jetbrains.plugins.bsp.ui.console.isBuildInProgress

class ResyncAction : SuspendableAction({ BspPluginBundle.message("resync.action.text") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}
