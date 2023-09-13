package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.runBuildTargetTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService

public class BuildTargetAction : AbstractActionWithTarget(
  BspPluginBundle.message("widget.build.target.popup.message")) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      target?.let { buildTarget(project, it) }
    } else {
      log.warn("BuildTargetAction cannot be performed! Project not available.")
    }
  }

  public companion object {
    private val log = logger<BuildTargetAction>()

    public fun buildTarget(project: Project, targetId: BuildTargetId) {
      BspCoroutineService.getInstance(project).start {
        runBuildTargetTask(listOf(targetId.toBsp4JTargetIdentifier()), project, log)
      }
    }
  }
}
