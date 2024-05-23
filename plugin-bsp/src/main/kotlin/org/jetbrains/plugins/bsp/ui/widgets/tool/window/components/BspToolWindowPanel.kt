package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolId
import org.jetbrains.plugins.bsp.services.InvalidTargetsProviderExtension
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import javax.swing.JComponent
import javax.swing.SwingConstants

private class ListsUpdater(
  private val project: Project,
  private val targetPanelUpdater: (ListsUpdater) -> Unit,
) {
  var loadedTargetsPanel: BspPanelComponent
    private set
  val targetFilter = TargetFilter { rerenderComponents() }
  val searchBarPanel = SearchBarPanel()

  init {
    val invalidTargets =
      InvalidTargetsProviderExtension.ep.withBuildToolId(project.buildToolId)?.provideInvalidTargets(project).orEmpty()
    val temporaryTargetUtils = project.temporaryTargetUtils
    loadedTargetsPanel =
      BspPanelComponent(
        targetIcon = project.assets.targetIcon,
        invalidTargetIcon = project.assets.invalidTargetIcon,
        buildToolId = project.buildToolId,
        toolName = project.assets.presentableName,
        targets = temporaryTargetUtils.allTargetIds().mapNotNull { temporaryTargetUtils.getBuildTargetInfoForId(it) },
        invalidTargets = invalidTargets,
        searchBarPanel = searchBarPanel,
      )
    loadedTargetsPanel.addMouseListener { LoadedTargetsMouseListener(it, project) }
    temporaryTargetUtils.registerListener { rerenderComponents() }
  }

  fun rerenderComponents() {
    val temporaryTargetUtils = project.temporaryTargetUtils
    searchBarPanel.clearAllListeners()
    loadedTargetsPanel = loadedTargetsPanel
      .createNewWithTargets(targetFilter.getMatchingLoadedTargets(temporaryTargetUtils))
    targetPanelUpdater(this@ListsUpdater)
  }
}

public class BspToolWindowPanel(project: Project) : SimpleToolWindowPanel(true, true) {
  init {
    val actionManager = ActionManager.getInstance()
    val listsUpdater = ListsUpdater(project, this::showCurrentPanel)

    val actionGroup = actionManager
      .getAction("Bsp.ActionsToolbar") as DefaultActionGroup

    actionGroup.addSeparator()
    actionGroup.add(FilterActionGroup(listsUpdater.targetFilter))

    val actionToolbar = actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true)
    actionToolbar.targetComponent = this.component
    actionToolbar.setOrientation(SwingConstants.HORIZONTAL)
    this.toolbar = actionToolbar.component

    showCurrentPanel(listsUpdater)
  }

  private fun showCurrentPanel(listsUpdater: ListsUpdater) {
    setToolWindowContent(listsUpdater.loadedTargetsPanel.wrappedInScrollPane())
  }

  private fun setToolWindowContent(component: JComponent) {
    this.setContent(component)
  }
}
