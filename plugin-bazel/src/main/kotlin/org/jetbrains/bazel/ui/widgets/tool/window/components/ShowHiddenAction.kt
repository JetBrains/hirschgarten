package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import org.jetbrains.bazel.config.BazelPluginBundle

internal class ShowHiddenAction(private val model: BazelTargetsPanelModel) : ToggleAction(
  BazelPluginBundle.message("widget.filter.show_hidden"),
  null,
  AllIcons.Actions.Show,
), DumbAware {
  override fun isSelected(e: AnActionEvent): Boolean = model.showHidden

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    model.showHidden = state
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
