package org.jetbrains.plugins.bsp.ui.actions.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.SyncProjectTask
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction

public class BuildAndResyncAction
: SuspendableAction({ BspPluginBundle.message("build.and.resync.action.text") }), DumbAware {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    SyncProjectTask(project).execute(
      shouldRunInitialSync = false,
      shouldBuildProject = true,
      shouldRunResync = true,
    )
  }
}
