package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.ListsUpdater
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

private class LoadTargetAction(
  text: String,
  private val target: BuildTargetIdentifier,
  private val listsUpdater: ListsUpdater,
) : AnAction(text) {

  override fun actionPerformed(e: AnActionEvent) {
    val diff = listsUpdater.magicMetaModel.loadTarget(target)
    runWriteAction { diff.applyOnWorkspaceModel() }
    listsUpdater.updateModels()
  }
}

public class NotLoadedTargetsMouseListener(
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
    val target: BuildTargetIdentifier? =
      BspTargetTree.getSelectedBspTarget(listsUpdater.notLoadedTargetsTreeComponent)?.id

    if (target != null) {
      val group = DefaultActionGroup()
      val action = LoadTargetAction(
        BspAllTargetsWidgetBundle.message("widget.load.target.popup.message"),
        target,
        listsUpdater
      )
      group.addAction(action)
      return group
    }
    return null
  }

  override fun mousePressed(e: MouseEvent?) {}

  override fun mouseReleased(e: MouseEvent?) {}

  override fun mouseEntered(e: MouseEvent?) {}

  override fun mouseExited(e: MouseEvent?) {}
}
