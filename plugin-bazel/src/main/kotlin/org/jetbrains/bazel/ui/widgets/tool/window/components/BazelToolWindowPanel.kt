package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeIntentReadAction
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle

internal fun configureBazelToolWindowToolBar(
  model: BazelTargetsPanelModel,
  actionManager: ActionManager,
  windowPanel: SimpleToolWindowPanel,
) {
  val defaultActions = actionManager.getAction("Bazel.ActionsToolbar")
  val actionGroup =
    DefaultActionGroup().apply {
      addAll(defaultActions)
      addSeparator()
      add(FilterActionGroup(model))
      addSeparator()
      add(BazelToolWindowSettingsAction(BazelPluginBundle.message("project.settings.display.name")))
      addSeparator()
      add(actionManager.getAction("Bazel.OpenProjectViewFile"))
    }

  val actionToolbar = actionManager.createActionToolbar("Bazel Toolbar", actionGroup, true)
  actionToolbar.targetComponent = windowPanel.component
  windowPanel.toolbar = actionToolbar.component
}

private class BazelToolWindowSettingsAction(private val settingsDisplayName: String) :
  SuspendableAction(
    { BazelPluginBundle.message("widget.settings.popup.message", settingsDisplayName) },
    AllIcons.General.Settings,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val showSettingsUtil = serviceAsync<ShowSettingsUtil>()
    withContext(Dispatchers.EDT) {
      writeIntentReadAction {
        showSettingsUtil.showSettingsDialog(project, settingsDisplayName)
      }
    }
  }
}
