package org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.Toggleable
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class FilterActionGroup(private val targetFilter: TargetFilter) :
  DefaultActionGroup(
    BspAllTargetsWidgetBundle.message("widget.filter.action.group"),
    null,
    AllIcons.General.Filter
  ),
  Toggleable {

  init {
    this.isPopup = true
    addFilterChangeAction(
      TargetFilter.FILTER.OFF,
      BspAllTargetsWidgetBundle.message("widget.filter.turn.off")
    )
    addFilterChangeAction(
      TargetFilter.FILTER.CAN_RUN,
      BspAllTargetsWidgetBundle.message("widget.filter.can.run")
    )
    addFilterChangeAction(
      TargetFilter.FILTER.CAN_TEST,
      BspAllTargetsWidgetBundle.message("widget.filter.can.test")
    )
  }

  private fun addFilterChangeAction(filterType: TargetFilter.FILTER, text: String) {
    this.add(FilterChangeAction(targetFilter, filterType, text))
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    Toggleable.setSelected(e.presentation, targetFilter.isFilterOn())
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

private class FilterChangeAction(
  private val targetFilter: TargetFilter,
  private val filterType: TargetFilter.FILTER,
  text: String
) : ToggleAction(text) {
  override fun isSelected(e: AnActionEvent): Boolean =
    targetFilter.currentFilter == filterType

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    if (state) {
      targetFilter.currentFilter = filterType
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
