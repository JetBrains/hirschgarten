package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeIntentReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.ui.widgets.tool.window.filter.FilterActionGroup
import java.nio.file.Path
import javax.swing.SwingConstants

class BazelToolWindowPanel(val project: Project) : SimpleToolWindowPanel(true, true) {
  private val model = project.service<BazelTargetsPanelModel>()
  val panel = BazelTargetsPanel(project, model)

  init {
    val actionManager = ActionManager.getInstance()

    val defaultActions = actionManager.getAction("Bazel.ActionsToolbar")
    val actionGroup =
      DefaultActionGroup().apply {
        addAll(defaultActions)
        addSeparator()
        add(FilterActionGroup(model))
        addSeparator()
        add(BazelToolWindowSettingsAction(BazelPluginBundle.message("project.settings.display.name")))
        addSeparator()
        add(BazelToolWindowConfigFileOpenAction())
      }

    val actionToolbar =
      actionManager.createActionToolbar("Bazel Toolbar", actionGroup, true).apply {
        targetComponent = this@BazelToolWindowPanel.component
        orientation = SwingConstants.HORIZONTAL
      }

    this.toolbar = actionToolbar.component
    setContent(panel)
  }
}

private class BazelToolWindowSettingsAction(private val settingsDisplayName: String) :
  SuspendableAction(
    { BazelPluginBundle.message("widget.settings.popup.message", settingsDisplayName) },
    AllIcons.General.Settings,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val showSettingsUtil = ShowSettingsUtil.getInstance()
    withContext(Dispatchers.EDT) {
      writeIntentReadAction {
        showSettingsUtil.showSettingsDialog(project, settingsDisplayName)
      }
    }
  }
}

private class BazelToolWindowConfigFileOpenAction :
  SuspendableAction(
    { BazelPluginBundle.message("widget.config.file.popup.message", BazelPluginBundle.message("tool.window.generic.config.file")) },
    AllIcons.FileTypes.Config,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val configFile = project.bazelProjectSettings.projectViewPath
    e.presentation.isEnabled = configFile != null
    withContext(Dispatchers.EDT) {
      ProjectView.getInstance(project).refresh()
      configFile?.getPsiFile(project)?.navigate(true)
    }
  }
}

private fun Path.getPsiFile(project: Project): PsiFile? {
  val virtualFileManager = VirtualFileManager.getInstance()
  val virtualFile =
    virtualFileManager.findFileByNioPath(this) ?: return null
  return PsiManager.getInstance(project).findFile(virtualFile)
}
