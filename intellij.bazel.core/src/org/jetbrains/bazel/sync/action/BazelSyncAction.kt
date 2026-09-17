package org.jetbrains.bazel.sync.action

import com.intellij.ide.trustedProjects.TrustedProjects
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.saveAllFiles
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.ProjectSyncScope
import org.jetbrains.bazel.sync.ProjectSyncService
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bazel.ui.console.isBuildInProgress
import javax.swing.Icon

internal abstract class BazelSyncAction : DumbAwareAction {
  constructor() : super()
  constructor(text: () -> String, icon: Icon?) : super(text, icon)

  protected abstract fun computeScope(project: Project, e: AnActionEvent): ProjectSyncScope?

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val scope = computeScope(project, e) ?: return
    BazelCoroutineService.getInstance(project).start {
      saveAllFiles()
      project.service<ProjectSyncService>().sync(scope)
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    if (project == null || !project.isBazelProject) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val visible = shouldBeVisible(project, e)
    e.presentation.isVisible = visible
    e.presentation.isEnabled = visible &&
                               TrustedProjects.isProjectTrusted(project) &&
                               !project.isSyncInProgress() &&
                               !project.isBuildInProgress()
    if (visible) {
      updatePresentation(project, e)
    }
  }

  protected open fun shouldBeVisible(project: Project, e: AnActionEvent): Boolean = true

  protected open fun updatePresentation(project: Project, e: AnActionEvent) {}

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
