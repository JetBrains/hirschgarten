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
import org.jetbrains.plugins.bsp.impl.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.services.InvalidTargetsProviderExtension
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import javax.swing.JComponent
import javax.swing.SwingConstants

private class ListsUpdater(private val project: Project, private val targetPanelUpdater: (ListsUpdater) -> Unit) {
  var loadedTargetsPanel: BspPanelComponent
    private set
  val targetFilter = TargetFilter { rerenderComponents() }
  val searchBarPanel = SearchBarPanel()

  init {
    val invalidTargets =
      InvalidTargetsProviderExtension.ep
        .withBuildToolId(project.buildToolId)
        ?.provideInvalidTargets(project)
        .orEmpty()
    val temporaryTargetUtils = project.temporaryTargetUtils
    loadedTargetsPanel =
      BspPanelComponent(
        targetIcon = project.assets.targetIcon,
        invalidTargetIcon = project.assets.errorTargetIcon,
        buildToolId = project.buildToolId,
        toolName = project.assets.presentableName,
        targets = temporaryTargetUtils.allTargetIds().mapNotNull { temporaryTargetUtils.getBuildTargetInfoForId(it) },
        invalidTargets = invalidTargets,
        searchBarPanel = searchBarPanel,
      )
    loadedTargetsPanel.addMouseListener { LoadedTargetsMouseListener(it, project) }
    temporaryTargetUtils.registerListener {
      ApplicationManager.getApplication().invokeLater {
        rerenderComponents()
      }
    }
  }

  fun rerenderComponents() {
    val temporaryTargetUtils = project.temporaryTargetUtils
    searchBarPanel.clearAllListeners()
    loadedTargetsPanel =
      loadedTargetsPanel
        .createNewWithTargets(targetFilter.getMatchingLoadedTargets(temporaryTargetUtils))
    targetPanelUpdater(this@ListsUpdater)
  }
}

public class BspToolWindowPanel(project: Project) : SimpleToolWindowPanel(true, true) {
  init {
    val actionManager = ActionManager.getInstance()
    val listsUpdater = ListsUpdater(project, this::showCurrentPanel)

    val defaultActions = actionManager.getAction("Bsp.ActionsToolbar")

    val actionGroup = DefaultActionGroup()
    actionGroup.addAll(defaultActions)
    actionGroup.addSeparator()
    actionGroup.add(FilterActionGroup(listsUpdater.targetFilter))
    actionGroup.addSettingsActionIfAvailable(project)

    val actionToolbar = actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true)
    actionToolbar.targetComponent = this.component
    actionToolbar.setOrientation(SwingConstants.HORIZONTAL)
    this.toolbar = actionToolbar.component

    showCurrentPanel(listsUpdater)
  }

  private fun DefaultActionGroup.addSettingsActionIfAvailable(project: Project) {
    val settingsActionProvider = project.bspToolWindowSettingsProvider ?: return
    addSeparator()
    add(BspToolWindowSettingsAction(settingsActionProvider.getSettingsName()))
  }

  private fun showCurrentPanel(listsUpdater: ListsUpdater) {
    setToolWindowContent(listsUpdater.loadedTargetsPanel.withScrollAndSearch())
  }

  private fun setToolWindowContent(component: JComponent) {
    this.setContent(component)
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
