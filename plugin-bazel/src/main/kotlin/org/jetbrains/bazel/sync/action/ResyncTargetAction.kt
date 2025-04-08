package org.jetbrains.bazel.sync.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
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

class ResyncTargetAction private constructor(private val targetId: Label) :
  SuspendableAction({ BazelPluginBundle.message("target.partial.sync.action.text") }, AllIcons.Actions.Refresh) {
    override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
      val syncScope = PartialProjectSync(targetsToSync = listOf(targetId))
      ProjectSyncTask(project).sync(syncScope = syncScope, buildProject = false)
    }

    override fun update(project: Project, e: AnActionEvent) {
      // for now we dont support jps modules (TODO: https://youtrack.jetbrains.com/issue/BAZEL-1238)
      val isJpsDisabled = !project.bazelJVMProjectSettings.enableBuildWithJps
      e.presentation.isVisible = BazelFeatureFlags.enablePartialSync && project.isBazelProject && isJpsDisabled
      e.presentation.isEnabled = !project.isSyncInProgress()
    }

    companion object {
      fun createIfEnabled(targetId: Label): ResyncTargetAction? =
        if (BazelFeatureFlags.enablePartialSync) {
          ResyncTargetAction(targetId)
        } else {
          null
        }
    }
  }
