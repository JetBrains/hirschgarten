package org.jetbrains.plugins.bsp.impl.actions.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.building.action.isBuildInProgress
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.impl.flow.sync.FullProjectSync
import org.jetbrains.plugins.bsp.impl.flow.sync.ProjectSyncTask
import org.jetbrains.plugins.bsp.impl.projectAware.isSyncInProgress

class BuildAndResyncAction : SuspendableAction({ BspPluginBundle.message("build.and.resync.action.text") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    ProjectSyncTask(project).sync(syncScope = FullProjectSync, buildProject = true)
  }

  override fun update(project: Project, e: AnActionEvent) {
    // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1237
    e.presentation.isVisible = project.buildToolId == BuildToolId("bazelbsp") // for now it's visible only for bazel-bsp projects
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}
