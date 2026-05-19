package org.jetbrains.bazel.sync.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.sync.task.ProjectSyncTask

internal class ResyncTargetAction private constructor(private val targetId: Label) :
  SuspendableAction({ BazelPluginBundle.message("target.partial.sync.action.text") }, AllIcons.Actions.Refresh) {
    override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
      val syncScope = PartialProjectSync(targetsToSync = listOf(targetId))
      ProjectSyncTask(project).sync(syncScope = syncScope, buildProject = false)
    }

    override fun update(project: Project, e: AnActionEvent) {
      e.presentation.isVisible = BazelFeatureFlags.enablePartialSync && project.isBazelProject
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
