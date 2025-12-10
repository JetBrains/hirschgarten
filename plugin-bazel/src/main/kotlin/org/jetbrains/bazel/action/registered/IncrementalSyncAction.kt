package org.jetbrains.bazel.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.sync_new.BazelSyncV2
import org.jetbrains.bazel.sync_new.flow.SyncBridgeService
import org.jetbrains.bazel.sync_new.flow.SyncScope
import org.jetbrains.bazel.ui.console.isBuildInProgress

class IncrementalSyncAction : SuspendableAction({ BazelPluginBundle.message("incremental_sync.action.text") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    if (BazelSyncV2.isEnabled) {
      project.service<SyncBridgeService>()
        .sync(scope = SyncScope.Incremental())
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isVisible = BazelSyncV2.isEnabled && project.isBazelProject
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}
