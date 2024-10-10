package org.jetbrains.plugins.bsp.runnerAction

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.server.tasks.runBuildTargetTask

public class BuildTargetAction(private val targetId: BuildTargetIdentifier) :
  SuspendableAction(
    text = { BspPluginBundle.message("widget.build.target.popup.message") },
    icon = AllIcons.Toolwindows.ToolWindowBuild,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    buildTarget(project, targetId)
  }

  public companion object {
    private val log = logger<BuildTargetAction>()

    public fun buildTarget(project: Project, targetId: BuildTargetIdentifier) {
      BspCoroutineService.getInstance(project).start {
        runBuildTargetTask(listOf(targetId), project, log)
      }
    }
  }
}
