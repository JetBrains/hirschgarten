package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.BuildTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RunTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.TestTargetAction
import javax.swing.JComponent

public class LoadedTargetsMouseListener(
  private val component: JComponent
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
    val target: BuildTarget? = BspTargetTree.getSelectedBspTarget(component)

    if (target != null) {
      val group = DefaultActionGroup()
      val actions = mutableListOf<AnAction>()
      if(target.capabilities.canCompile) {
        actions.add(BuildTargetAction(target.id))
      }
      if(target.capabilities.canRun) {
        actions.add(RunTargetAction(target.id))
      }
      if(target.capabilities.canTest) {
        actions.add(TestTargetAction(target.id))
      }
      group.addAll(actions)
      return group
    }

    return null
  }

  override fun mousePressed(e: MouseEvent?) { }

  override fun mouseReleased(e: MouseEvent?) { }

  override fun mouseEntered(e: MouseEvent?) { }

  override fun mouseExited(e: MouseEvent?) { }
}
