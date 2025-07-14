package org.jetbrains.bazel.runnerAction

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.server.tasks.runBuildTargetTask

class BuildTargetAction(private val targetId: CanonicalLabel) :
  SuspendableAction(
    text = { BazelPluginBundle.message("widget.build.target.popup.message") },
    icon = AllIcons.Toolwindows.ToolWindowBuild,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    buildTarget(project, targetId)
  }

  companion object {
    fun buildTarget(project: Project, targetId: CanonicalLabel) {
      BazelCoroutineService.getInstance(project).start {
        runBuildTargetTask(listOf(targetId), project)
      }
    }
  }
}
