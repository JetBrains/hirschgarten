package org.jetbrains.bazel.runnerAction

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.coroutines.BspCoroutineService
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bsp.protocol.BuildTargetIdentifier

class BuildTargetAction(private val targetId: BuildTargetIdentifier) :
  SuspendableAction(
    text = { BspPluginBundle.message("widget.build.target.popup.message") },
    icon = AllIcons.Toolwindows.ToolWindowBuild,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    buildTarget(project, targetId)
  }

  companion object {
    fun buildTarget(project: Project, targetId: BuildTargetIdentifier) {
      BspCoroutineService.getInstance(project).start {
        runBuildTargetTask(listOf(targetId), project)
      }
    }
  }
}
