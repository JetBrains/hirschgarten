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

internal abstract class BazelSyncActionBase(text: () -> String) : SuspendableAction(text) {
  protected abstract val scope: ProjectSyncScope

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    project.service<ProjectSyncService>().sync(scope)
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}

internal class BuildAndResyncAction : BazelSyncActionBase({ BazelPluginBundle.message("build.and.resync.action.text") }) {
  override val scope = ProjectSyncScope.Full(build = true, phased = false)
}

internal class ResyncAction : BazelSyncActionBase({ BazelPluginBundle.message("resync.action.text") }) {
  override val scope = ProjectSyncScope.Full(build = false, phased = false)
}
