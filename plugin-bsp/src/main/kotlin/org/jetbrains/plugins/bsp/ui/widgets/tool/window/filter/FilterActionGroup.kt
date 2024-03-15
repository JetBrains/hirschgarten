package org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Toggleable
import org.jetbrains.plugins.bsp.config.BspPluginBundle

public class FilterActionGroup(
  private val targetFilter: TargetFilter,
) : DefaultActionGroup(
  BspPluginBundle.message("widget.filter.action.group"),
  null,
  AllIcons.General.Filter,
), Toggleable {
  init {
    this.isPopup = true
    add(ClearFiltersAction(targetFilter))
    addSeparator(BspPluginBundle.message("widget.filter.capability"))
    addAll(capabilityFilterActions())
    addSeparator(BspPluginBundle.message("widget.filter.state"))
    addAll(stateFilterActions())
  }

  private fun capabilityFilterActions(): List<AnAction> =
    listOf(
      TargetFilter.ByCapability.CAN_RUN to BspPluginBundle.message("widget.filter.capability.run"),
      TargetFilter.ByCapability.CAN_TEST to BspPluginBundle.message("widget.filter.capability.test"),
    ).map {
      FilterChangeAction.Capability(
        targetFilter = targetFilter,
        thisActionsCapabilityFilter = it.first,
        text = it.second,
      )
    }

  private fun stateFilterActions(): List<AnAction> =
    listOf(
      TargetFilter.ByState.LOADED to BspPluginBundle.message("widget.filter.state.loaded"),
      TargetFilter.ByState.UNLOADED to BspPluginBundle.message("widget.filter.state.unloaded"),
      TargetFilter.ByState.INVALID to BspPluginBundle.message("widget.filter.state.invalid"),
    ).map {
      FilterChangeAction.State(
        targetFilter = targetFilter,
        thisActionsStateFilter = it.first,
        text = it.second,
      )
    }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    super.update(e)
    Toggleable.setSelected(e.presentation, targetFilter.areFiltersEnabled())
  }
}

private class ClearFiltersAction(
  private val targetFilter: TargetFilter,
) : AnAction(BspPluginBundle.message("widget.filter.clear")) {
  override fun actionPerformed(event: AnActionEvent) {
    targetFilter.clearFilters()
  }

  override fun update(event: AnActionEvent) {
    super.update(event)
    event.presentation.isEnabled = targetFilter.areFiltersEnabled()
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
