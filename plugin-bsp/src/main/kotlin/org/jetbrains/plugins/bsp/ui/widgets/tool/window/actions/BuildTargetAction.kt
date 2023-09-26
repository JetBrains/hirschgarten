package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.runBuildTargetTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction

public class BuildTargetAction(
  private val targetId: BuildTargetId,
) : SuspendableAction(
  text = { BspPluginBundle.message("widget.build.target.popup.message") },
  icon = AllIcons.Toolwindows.ToolWindowBuild,
) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    buildTarget(project, targetId)
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
