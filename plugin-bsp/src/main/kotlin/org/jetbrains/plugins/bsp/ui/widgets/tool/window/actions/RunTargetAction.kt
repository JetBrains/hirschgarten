package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.services.BspBuildConsoleService
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class RunTargetAction(
  private val target: BuildTargetIdentifier
  ) : AnAction(BspAllTargetsWidgetBundle.message("widget.run.target.popup.message")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val bspConnectionService = project.getService(BspConnectionService::class.java)
    val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
    val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)

    val bspResolver = VeryTemporaryBspResolver(
      project.stateStore.projectBasePath,
      bspConnectionService.server!!,
      bspSyncConsoleService.bspSyncConsole,
      bspBuildConsoleService.bspBuildConsole
    )
    bspResolver.runTarget(target)
    runBackgroundableTask("Run single target", project) {
    }
  }
}