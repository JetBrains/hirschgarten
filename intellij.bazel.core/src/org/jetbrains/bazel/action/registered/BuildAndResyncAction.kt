package org.jetbrains.bazel.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.ui.console.isBuildInProgress

internal class BuildAndResyncAction : SuspendableAction({ BazelPluginBundle.message("build.and.resync.action.text") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    project.service<ProjectSyncService>().sync(ProjectSyncScope.Full(build = true, phased = false))
  }

  override fun update(project: Project, e: AnActionEvent) {
    // TODO: https://youtrack.jetbrains.com/issue/BAZEL-1237
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}
