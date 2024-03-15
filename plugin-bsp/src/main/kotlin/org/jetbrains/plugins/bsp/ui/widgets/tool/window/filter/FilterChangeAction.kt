package org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

public sealed class FilterChangeAction<F : TargetFilter.FilterType>(
  protected val targetFilter: TargetFilter,
  private val thisActionsFilter: F,
  text: String,
) : ToggleAction(text) {
  protected abstract fun getOffFilter(): F

  protected abstract fun getCurrentFilter(): F

  override fun isSelected(event: AnActionEvent): Boolean =
    getCurrentFilter() == thisActionsFilter

  override fun setSelected(event: AnActionEvent, newSelection: Boolean) {
    if (newSelection) {
      setFilter(thisActionsFilter)
    } else {
      setFilter(getOffFilter()) // clicking the current filter disables it
    }
  }

  private fun setFilter(newFilter: F) {
    targetFilter.updateFilter(newFilter)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  public class State(
    targetFilter: TargetFilter,
    thisActionsStateFilter: TargetFilter.ByState,
    text: String,
  ) : FilterChangeAction<TargetFilter.ByState>(targetFilter, thisActionsStateFilter, text) {
    override fun getOffFilter(): TargetFilter.ByState = TargetFilter.ByState.OFF

    override fun getCurrentFilter(): TargetFilter.ByState = targetFilter.currentStateFilter
  }

  public class Capability(
    targetFilter: TargetFilter,
    thisActionsCapabilityFilter: TargetFilter.ByCapability,
    text: String,
  ) : FilterChangeAction<TargetFilter.ByCapability>(targetFilter, thisActionsCapabilityFilter, text) {
    override fun getOffFilter(): TargetFilter.ByCapability = TargetFilter.ByCapability.OFF

    override fun getCurrentFilter(): TargetFilter.ByCapability = targetFilter.currentCapabilityFilter
  }
}
