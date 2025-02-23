package org.jetbrains.bazel.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.ui.console.isBuildInProgress

class BuildAndResyncAction : SuspendableAction({ BspPluginBundle.message("build.and.resync.action.text") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = true)
  }

  override fun update(project: Project, e: AnActionEvent) {
    // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1237
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}
