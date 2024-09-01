package org.jetbrains.plugins.bsp.restOfTheFuckingHorse.actions.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.building.action.isBuildInProgress
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspSyncStatusService
import org.jetbrains.plugins.bsp.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.action.SuspendableAction

public class ResyncAction :
  SuspendableAction({ BspPluginBundle.message("resync.action.text") }),
  DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    ProjectSyncTask(project).sync(buildProject = false)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}

internal fun Project.isSyncInProgress() = BspSyncStatusService.getInstance(this).isSyncInProgress
