package org.jetbrains.plugins.bsp.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.buildToolIdOrNull
import org.jetbrains.plugins.bsp.sync.scope.SecondPhaseSync
import org.jetbrains.plugins.bsp.sync.status.isSyncInProgress
import org.jetbrains.plugins.bsp.sync.task.ProjectSyncTask
import org.jetbrains.plugins.bsp.ui.console.isBuildInProgress

class BuildAndResyncAction : SuspendableAction({ BspPluginBundle.message("build.and.resync.action.text") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = true)
  }

  override fun update(project: Project, e: AnActionEvent) {
    // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1237
    e.presentation.isVisible = project.buildToolIdOrNull == BuildToolId("bazelbsp") // for now it's visible only for bazel-bsp projects
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}
