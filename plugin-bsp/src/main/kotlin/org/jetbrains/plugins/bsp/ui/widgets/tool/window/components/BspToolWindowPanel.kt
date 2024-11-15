package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

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
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId
import org.jetbrains.plugins.bsp.extensionPoints.BspToolWindowConfigFileProviderExtension
import org.jetbrains.plugins.bsp.extensionPoints.bspToolWindowConfigFileProvider
import org.jetbrains.plugins.bsp.extensionPoints.bspToolWindowSettingsProvider
import org.jetbrains.plugins.bsp.services.InvalidTargetsProviderExtension
import org.jetbrains.plugins.bsp.target.TemporaryTargetUtils
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import java.nio.file.Path
import javax.swing.SwingConstants

class BspToolWindowPanel(val project: Project) : SimpleToolWindowPanel(true, true) {
  private val targetFilter = TargetFilter { rerenderComponents() }
  private val searchBarPanel = SearchBarPanel()
  private var loadedTargetsPanel: BspPanelComponent

  init {
    val actionManager = ActionManager.getInstance()
    val temporaryTargetUtils = project.temporaryTargetUtils

    loadedTargetsPanel = createLoadedTargetsPanel(project, temporaryTargetUtils)

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

    temporaryTargetUtils.registerSyncListener { targetListChanged ->
      if (targetListChanged) {
        ApplicationManager.getApplication().invokeLater {
          rerenderComponents()
        }
      }
    }
  }

  private fun createLoadedTargetsPanel(project: Project, temporaryTargetUtils: TemporaryTargetUtils): BspPanelComponent {
    val invalidTargets =
      InvalidTargetsProviderExtension.ep
        .withBuildToolId(project.buildToolId)
        ?.provideInvalidTargets(project)
        .orEmpty()

    return BspPanelComponent(
      targetIcon = project.assets.targetIcon,
      invalidTargetIcon = project.assets.errorTargetIcon,
      buildToolId = project.buildToolId,
      toolName = project.assets.presentableName,
      targets = temporaryTargetUtils.allTargetIds().mapNotNull { temporaryTargetUtils.getBuildTargetInfoForId(it) },
      invalidTargets = invalidTargets,
      searchBarPanel = searchBarPanel,
    ).apply {
      registerPopupHandler { LoadedTargetsMouseListener(it, project) }
    }
  }

  private fun rerenderComponents() {
    val temporaryTargetUtils = project.temporaryTargetUtils
    searchBarPanel.clearAllListeners()
    loadedTargetsPanel = loadedTargetsPanel.createNewWithTargets(targetFilter.getMatchingLoadedTargets(temporaryTargetUtils))
    setContent(loadedTargetsPanel.withScrollAndSearch())
  }

  private fun DefaultActionGroup.addSettingsActionIfAvailable(project: Project) {
    val settingsActionProvider = project.bspToolWindowSettingsProvider ?: return
    addSeparator()
    add(BspToolWindowSettingsAction(settingsActionProvider.getSettingsName()))
  }

  private fun DefaultActionGroup.addConfigFileOpeningActionIfAvailable(project: Project) {
    val configFileProvider = project.bspToolWindowConfigFileProvider ?: return
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

private class BspToolWindowConfigFileOpenAction(private val configFileProvider: BspToolWindowConfigFileProviderExtension) :
  SuspendableAction(
    { BspPluginBundle.message("widget.config.file.popup.message", configFileProvider.getConfigFileGenericName()) },
    AllIcons.Actions.MenuOpen,
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
