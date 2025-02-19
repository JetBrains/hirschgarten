package org.jetbrains.bazel.ui.widgets.tool.window.components

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeIntentReadAction
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.assets.assets
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.config.buildToolId
import org.jetbrains.bazel.extensionPoints.ToolWindowConfigFileProviderExtension
import org.jetbrains.bazel.extensionPoints.toolWindowConfigFileProvider
import org.jetbrains.bazel.extensionPoints.toolWindowSettingsProvider
import org.jetbrains.bazel.services.invalidTargets
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.bazel.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.bazel.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.bazel.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import java.nio.file.Path
import javax.swing.SwingConstants

class BspToolWindowPanel(val project: Project) : SimpleToolWindowPanel(true, true) {
  private val targetFilter = TargetFilter { rerenderComponents() }
  private val searchBarPanel = SearchBarPanel()
  private var loadedTargetsPanel: BspPanelComponent

  init {
    val actionManager = ActionManager.getInstance()
    val targetUtils = project.targetUtils

    loadedTargetsPanel = createLoadedTargetsPanel(project, targetUtils)

    val defaultActions = actionManager.getAction("Bsp.ActionsToolbar")
    val actionGroup =
      DefaultActionGroup().apply {
        addAll(defaultActions)
        addSeparator()
        add(FilterActionGroup(targetFilter))
        addSettingsActionIfAvailable(project)
        addConfigFileOpeningActionIfAvailable(project)
      }

    val actionToolbar =
      actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true).apply {
        targetComponent = this@BspToolWindowPanel.component
        orientation = SwingConstants.HORIZONTAL
      }

    this.toolbar = actionToolbar.component
    setContent(loadedTargetsPanel.withScrollAndSearch())

    targetUtils.registerSyncListener { targetListChanged ->
      if (targetListChanged) {
        ApplicationManager.getApplication().invokeLater {
          rerenderComponents()
        }
      }
    }
  }

  private fun createLoadedTargetsPanel(project: Project, targetUtils: TargetUtils): BspPanelComponent =
    BspPanelComponent(
      targetIcon = project.assets.targetIcon,
      invalidTargetIcon = project.assets.errorTargetIcon,
      buildToolId = project.buildToolId,
      toolName = project.assets.presentableName,
      targets = targetUtils.allTargets().mapNotNull { targetUtils.getBuildTargetInfoForLabel(it) },
      invalidTargets = project.invalidTargets,
      searchBarPanel = searchBarPanel,
    ).apply {
      registerPopupHandler { LoadedTargetsMouseListener(it, project) }
    }

  private fun rerenderComponents() {
    val targetUtils = project.targetUtils
    searchBarPanel.clearAllListeners()
    loadedTargetsPanel =
      loadedTargetsPanel.createNewWithTargets(targetFilter.getMatchingLoadedTargets(targetUtils), project.invalidTargets)
    setContent(loadedTargetsPanel.withScrollAndSearch())
  }

  private fun DefaultActionGroup.addSettingsActionIfAvailable(project: Project) {
    val settingsActionProvider = project.toolWindowSettingsProvider ?: return
    addSeparator()
    add(BspToolWindowSettingsAction(settingsActionProvider.getSettingsName()))
  }

  private fun DefaultActionGroup.addConfigFileOpeningActionIfAvailable(project: Project) {
    val configFileProvider = project.toolWindowConfigFileProvider ?: return
    addSeparator()
    add(BspToolWindowConfigFileOpenAction(configFileProvider))
  }
}

private class BspToolWindowSettingsAction(private val settingsDisplayName: String) :
  SuspendableAction(
    { BspPluginBundle.message("widget.settings.popup.message", settingsDisplayName) },
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

private class BspToolWindowConfigFileOpenAction(private val configFileProvider: ToolWindowConfigFileProviderExtension) :
  SuspendableAction(
    { BspPluginBundle.message("widget.config.file.popup.message", configFileProvider.getConfigFileGenericName()) },
    configFileProvider.getConfigFileIcon(),
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val configFile = configFileProvider.getConfigFile(project)
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
