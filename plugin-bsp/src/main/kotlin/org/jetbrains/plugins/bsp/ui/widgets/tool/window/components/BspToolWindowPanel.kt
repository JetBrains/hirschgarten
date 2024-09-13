package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.config.withBuildToolId
import org.jetbrains.plugins.bsp.extensionPoints.bspToolWindowSettingsProvider
import org.jetbrains.plugins.bsp.impl.target.TemporaryTargetUtils
import org.jetbrains.plugins.bsp.impl.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.services.InvalidTargetsProviderExtension
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import javax.swing.SwingConstants

public class BspToolWindowPanel(val project: Project) : SimpleToolWindowPanel(true, true) {
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
      }

    val actionToolbar =
      actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true).apply {
        targetComponent = this@BspToolWindowPanel.component
        orientation = SwingConstants.HORIZONTAL
      }

    this.toolbar = actionToolbar.component
    setContent(loadedTargetsPanel.withScrollAndSearch())

    temporaryTargetUtils.registerListener {
      ApplicationManager.getApplication().invokeLater {
        rerenderComponents()
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
      addMouseListener { LoadedTargetsMouseListener(it, project) }
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
}

private class BspToolWindowSettingsAction(private val settingsDisplayName: String) :
  DumbAwareAction(
    { BspPluginBundle.message("widget.settings.popup.message", settingsDisplayName) },
    AllIcons.General.Settings,
  ) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val showSettingsUtil = ShowSettingsUtil.getInstance()
    showSettingsUtil.showSettingsDialog(project, settingsDisplayName)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
