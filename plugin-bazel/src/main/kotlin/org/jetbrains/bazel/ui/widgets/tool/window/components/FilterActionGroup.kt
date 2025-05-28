package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.project.DumbAware
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.ui.widgets.tool.window.components.BazelTargetsPanelModel
import org.jetbrains.bsp.protocol.BuildTarget

enum class TargetFilter(public val predicate: (BuildTarget) -> Boolean) {
  OFF({ true }),
  CAN_RUN({ it.kind.ruleType == RuleType.BINARY }),
  CAN_TEST({ it.kind.ruleType == RuleType.TEST }),
}

class FilterActionGroup(private val model: BazelTargetsPanelModel) :
  DefaultActionGroup(
    BazelPluginBundle.message("widget.filter.action.group"),
    null,
    AllIcons.General.Filter,
  ),
  Toggleable,
  DumbAware {
  init {
    this.isPopup = true
    addFilterChangeAction(
      TargetFilter.OFF,
      BazelPluginBundle.message("widget.filter.turn.off"),
    )
    addFilterChangeAction(
      TargetFilter.CAN_RUN,
      BazelPluginBundle.message("widget.filter.can.run"),
    )
    addFilterChangeAction(
      TargetFilter.CAN_TEST,
      BazelPluginBundle.message("widget.filter.can.test"),
    )
  }

  private fun addFilterChangeAction(filterType: TargetFilter, text: String) {
    this.add(FilterChangeAction(model, filterType, text))
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    Toggleable.setSelected(e.presentation, model.targetFilter != TargetFilter.OFF)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

private class FilterChangeAction(
  private val model: BazelTargetsPanelModel,
  private val filterType: TargetFilter,
  text: String,
) : ToggleAction(text),
  DumbAware {
  override fun isSelected(e: AnActionEvent): Boolean = model.targetFilter == filterType

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    if (state) {
      model.targetFilter = filterType
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
