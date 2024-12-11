package org.jetbrains.plugins.bsp.impl.actions.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.building.action.isBuildInProgress
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.impl.flow.sync.SecondPhaseSync
import org.jetbrains.plugins.bsp.impl.projectAware.isSyncInProgress

public class ResyncAction : SuspendableAction({ BspPluginBundle.message("resync.action.text") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}
