package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.AbstractActionWithTarget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.BuildTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RunTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.TestTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetContainer
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

public class LoadedTargetsMouseListener(
  private val container: BuildTargetContainer,
) : MouseListener {

  override fun mouseClicked(e: MouseEvent?) {
    e?.let { mouseClickedNotNull(it) }
  }

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
    val target = container.getSelectedBuildTarget()

    return if (target != null) {
      val actions = mutableListOf<AnAction>()
      if (target.capabilities.canCompile) {
        val action = getAction(BuildTargetAction::class.java)
        actions.add(action)
      }
      if (target.capabilities.canRun) {
        val action = getAction(RunTargetAction::class.java)
        actions.add(action)
      }
      if (target.capabilities.canTest) {
        val action = getAction(TestTargetAction::class.java)
        actions.add(action)
      }
      DefaultActionGroup().also { it.addAll(actions) }
    } else null
  }

  private fun getAction(actionClass : Class<out AbstractActionWithTarget>) : AbstractActionWithTarget =
    actions.getOrPut(actionClass) {
      actionClass.constructors.first { it.parameterCount == 0 }.newInstance() as AbstractActionWithTarget
    }.also { it.target = container.getSelectedBuildTarget()?.id }

  override fun mousePressed(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseReleased(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseEntered(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseExited(e: MouseEvent?) { /* nothing to do */ }

  private companion object {
    val actions = HashMap<Class<out AbstractActionWithTarget>, AbstractActionWithTarget>()
  }
}
