package org.jetbrains.bazel.action.registered

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject

class ToggleBazelLogAction :
  DumbAwareToggleAction(BazelPluginBundle.message("action.toggle.bazel.log.text")) {

  override fun isSelected(e: AnActionEvent): Boolean = Registry.`is`(BazelFeatureFlags.ENABLE_LOG)

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    Registry.get(BazelFeatureFlags.ENABLE_LOG).setValue(state)
  }

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = e.project?.isBazelProject == true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
