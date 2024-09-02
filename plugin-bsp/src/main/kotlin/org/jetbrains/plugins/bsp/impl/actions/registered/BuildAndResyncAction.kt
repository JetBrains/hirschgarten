package org.jetbrains.plugins.bsp.impl.actions.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.building.action.isBuildInProgress
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask

public class BuildAndResyncAction :
  SuspendableAction({ BspPluginBundle.message("build.and.resync.action.text") }),
  DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    ProjectSyncTask(project).sync(buildProject = true)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}
