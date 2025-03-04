package org.jetbrains.bazel.runnerAction

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.tasks.runBuildTargetTask

class BuildTargetAction(private val targetId: Label) :
  SuspendableAction(
    text = { BspPluginBundle.message("widget.build.target.popup.message") },
    icon = AllIcons.Toolwindows.ToolWindowBuild,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    buildTarget(project, targetId)
  }

  companion object {
    fun buildTarget(project: Project, targetId: Label) {
      BazelCoroutineService.getInstance(project).start {
        runBuildTargetTask(listOf(targetId), project)
      }
    }
  }
}
