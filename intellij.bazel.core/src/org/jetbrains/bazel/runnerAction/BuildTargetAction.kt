package org.jetbrains.bazel.runnerAction

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.task.ProjectTaskManager
import com.intellij.workspaceModel.ide.legacyBridge.findModule
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.tasks.runBuildTargetTask
import org.jetbrains.bazel.target.ModuleTargetService

internal class BuildTargetAction(private val targetId: Label) :
  SuspendableAction(
    text = { BazelPluginBundle.message("widget.build.target.popup.message") },
    icon = AllIcons.Toolwindows.ToolWindowBuild,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    buildTarget(project, targetId)
  }

  companion object {
    fun buildTarget(project: Project, targetId: Label) {
      BazelCoroutineService.getInstance(project).start {
        val modules = project.service<ModuleTargetService>().findLegacyModulesByLabel(label = targetId)
        if (modules.isNotEmpty()) {
          // Run through ProjectTaskManager to make sure hotswap works
          ProjectTaskManager.getInstance(project).build(*modules.toTypedArray())
        }
        else {
          runBuildTargetTask(listOf(targetId), project)
        }
      }
    }
  }
}
