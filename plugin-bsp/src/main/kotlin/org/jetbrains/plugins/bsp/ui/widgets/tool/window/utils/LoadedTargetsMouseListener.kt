package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.ListsUpdater
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import org.jetbrains.plugins.bsp.services.BspBuildConsoleService
import org.jetbrains.plugins.bsp.services.BspSyncConsoleService
import org.jetbrains.plugins.bsp.services.VeryTemporaryBspResolver

private class BuildTargetAction(
  text: String,
  private val target: BuildTargetIdentifier
) : AnAction(text) {

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
    runBackgroundableTask("Build single target", project) {
      bspResolver.buildTarget(target)
    }
  }
}

public class LoadedTargetsMouseListener(
  private val listsUpdater: ListsUpdater,
) : MouseListener {

  override fun mouseClicked(e: MouseEvent?): Unit = mouseClickedNotNull(e!!)

  private fun mouseClickedNotNull(mouseEvent: MouseEvent) {
    if (mouseEvent.mouseButton == MouseButton.Right) {
      showPopup(mouseEvent)
    }
  }

  private fun showPopup(mouseEvent: MouseEvent) {
    val actionGroup = calculatePopupGroup()
    if (actionGroup != null) {
      val context = DataManager.getInstance().getDataContext(mouseEvent.component)
      val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS

      JBPopupFactory.getInstance()
        .createActionGroupPopup(null, actionGroup, context, mnemonics, true)
        .showInBestPositionFor(context)
    }
  }

  private fun calculatePopupGroup(): ActionGroup? {
    val target: BuildTargetIdentifier? = BspTargetTree.getSelectedBspTarget(listsUpdater.loadedTargetsTreeComponent)?.id

    if (target != null) {
      val group = DefaultActionGroup()
      val action = BuildTargetAction(
        BspAllTargetsWidgetBundle.message("widget.build.target.popup.message"),
        target
      )
      group.addAction(action)
      return group
    }

    return null
  }

  override fun mousePressed(e: MouseEvent?) { }

  override fun mouseReleased(e: MouseEvent?) { }

  override fun mouseEntered(e: MouseEvent?) { }

  override fun mouseExited(e: MouseEvent?) { }
}
