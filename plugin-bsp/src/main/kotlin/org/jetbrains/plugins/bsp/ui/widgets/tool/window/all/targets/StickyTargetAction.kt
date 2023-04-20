package org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import javax.swing.Icon

/**
 * An action which will be presented with a sticky button (one that might stay selected after clicking)
 *
 * @property onPerform function to be performed upon clicking this action's button
 * @property selectionProvider function deciding whether the button is to be displayed as selected or not
 *
 * @param hintText text to be displayed on hover
 * @param icon an icon for this action's button
 */
public class StickyTargetAction(
  hintText: String,
  icon: Icon,
  private val onPerform: () -> Unit,
  private val selectionProvider: () -> Boolean
) : AnAction({ hintText }, icon), Toggleable {

  override fun actionPerformed(e: AnActionEvent) {
    onPerform()
    update(e)
  }

  override fun update(e: AnActionEvent): Unit =
    Toggleable.setSelected(e.presentation, selectionProvider())

  override fun getActionUpdateThread(): ActionUpdateThread =
    ActionUpdateThread.EDT
}
