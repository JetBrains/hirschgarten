package org.jetbrains.bazel.sync.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.sync_new.flow.PartialTarget
import org.jetbrains.bazel.sync_new.flow.SyncBridgeService
import org.jetbrains.bazel.sync_new.flow.SyncScope
import org.jetbrains.bazel.sync_new.flow.SyncSpec
import org.jetbrains.bazel.sync_new.isNewSyncEnabled

// TODO: refactor this action to use DataContext and don't use constructor with parameters
class ResyncTargetAction private constructor(private val targetId: Label) :
  SuspendableAction({ BazelPluginBundle.message("target.partial.sync.action.text") }, AllIcons.Actions.Refresh) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    if (project.isNewSyncEnabled) {
      val scope = SyncScope.Partial(targets = listOf(PartialTarget.ByLabel(targetId)))
      project.service<SyncBridgeService>().sync(spec = SyncSpec(), scope = scope)
    } else {
      val syncScope = PartialProjectSync(targetsToSync = listOf(targetId))
      ProjectSyncTask(project).sync(syncScope = syncScope, buildProject = false)
    }
  }

  override fun update(project: Project, e: AnActionEvent) {
    // for now we dont support jps modules (TODO: https://youtrack.jetbrains.com/issue/BAZEL-1238)
    val isJpsDisabled = !project.bazelJVMProjectSettings.enableBuildWithJps
    if (project.isNewSyncEnabled) {
      e.presentation.isVisible = true
    } else {
      e.presentation.isVisible = BazelFeatureFlags.enablePartialSync && project.isBazelProject && isJpsDisabled
    }
    e.presentation.isEnabled = !project.isSyncInProgress()
  }

  companion object {
    fun createIfEnabled(project: Project, targetId: Label): ResyncTargetAction? =
      if (BazelFeatureFlags.enablePartialSync || project.isNewSyncEnabled) {
        ResyncTargetAction(targetId)
      } else {
        null
      }
  }
}
