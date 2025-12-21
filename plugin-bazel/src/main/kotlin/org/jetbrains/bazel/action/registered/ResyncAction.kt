package org.jetbrains.bazel.action.registered

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.sync_new.SyncFlagsService
import org.jetbrains.bazel.sync_new.flow.SyncBridgeService
import org.jetbrains.bazel.sync_new.flow.SyncScope
import org.jetbrains.bazel.sync_new.isNewSyncEnabled
import org.jetbrains.bazel.ui.console.isBuildInProgress

class ResyncAction : SuspendableAction({ BazelPluginBundle.message("resync.action.text") }) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    if (project.isNewSyncEnabled) {
      project.service<SyncBridgeService>()
        .sync(scope = SyncScope.Full())
    } else {
      ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
  }
}
