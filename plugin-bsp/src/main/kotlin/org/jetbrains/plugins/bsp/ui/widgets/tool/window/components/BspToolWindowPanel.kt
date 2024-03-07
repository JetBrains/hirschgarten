package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.services.InvalidTargetsProviderExtension
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.StickyTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.TargetFilter
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.search.SearchBarPanel
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.NotLoadedTargetsMouseListener
import javax.swing.JComponent
import javax.swing.SwingConstants

private enum class PanelShown {
  LOADED,
  NOTLOADED,
}

private class ListsUpdater(
  private val project: Project,
  private val targetPanelUpdater: (ListsUpdater) -> Unit,
) {
  var loadedTargetsPanel: BspPanelComponent
    private set
  var notLoadedTargetsPanel: BspPanelComponent
    private set
  val targetFilter = TargetFilter { rerenderComponents() }
  val searchBarPanel = SearchBarPanel()

  init {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
    val invalidTargets =
      InvalidTargetsProviderExtension.ep.withBuildToolId(project.buildToolId)?.provideInvalidTargets(project).orEmpty()

    loadedTargetsPanel =
      BspPanelComponent(
        targetIcon = assetsExtension.loadedTargetIcon,
        invalidTargetIcon = assetsExtension.invalidTargetIcon,
        buildToolId = project.buildToolId,
        toolName = assetsExtension.presentableName,
        targets = magicMetaModel.getAllLoadedTargets(),
        invalidTargets = invalidTargets,
        searchBarPanel = searchBarPanel,
      )
    loadedTargetsPanel.addMouseListener { LoadedTargetsMouseListener(it, project) }

    notLoadedTargetsPanel =
      BspPanelComponent(
        targetIcon = assetsExtension.unloadedTargetIcon,
        invalidTargetIcon = assetsExtension.invalidTargetIcon,
        buildToolId = project.buildToolId,
        toolName = assetsExtension.presentableName,
        targets = magicMetaModel.getAllNotLoadedTargets(),
        invalidTargets = emptyList(),
        searchBarPanel = searchBarPanel,
      )
    notLoadedTargetsPanel.addMouseListener { NotLoadedTargetsMouseListener(it, project) }
    magicMetaModel.registerTargetLoadListener { rerenderComponents() }
  }

  fun rerenderComponents() {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    searchBarPanel.clearAllListeners()
    loadedTargetsPanel = loadedTargetsPanel.createNewWithTargets(targetFilter.getMatchingLoadedTargets(magicMetaModel))
    notLoadedTargetsPanel = notLoadedTargetsPanel.createNewWithTargets(
      targetFilter.getMatchingNotLoadedTargets(magicMetaModel))
    targetPanelUpdater(this@ListsUpdater)
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {
  private var panelShown = PanelShown.LOADED

  public constructor(project: Project) : this() {
    val actionManager = ActionManager.getInstance()
    val listsUpdater = ListsUpdater(project, this::showCurrentPanel)

    val actionGroup = actionManager
      .getAction("Bsp.ActionsToolbar") as DefaultActionGroup

    val notLoadedTargetsActionName = BspPluginBundle.message("widget.not.loaded.targets.tab.name")
    val loadedTargetsActionName = BspPluginBundle.message("widget.loaded.targets.tab.name")

    actionGroup.childActionsOrStubs.iterator().forEach {
      if (it.shouldBeDisposedAfterReload()) {
        actionGroup.remove(it)
      }
    }

    actionGroup.addSeparator()
    actionGroup.add(
      StickyTargetAction(
        hintText = notLoadedTargetsActionName,
        icon = BspPluginIcons.unloadedTargetsFilterIcon,
        onPerform = { listsUpdater.showNotLoadedTargets() },
        selectionProvider = { panelShown == PanelShown.NOTLOADED },
      )
    )
    actionGroup.add(
      StickyTargetAction(
        hintText = loadedTargetsActionName,
        icon = BspPluginIcons.loadedTargetsFilterIcon,
        onPerform = { listsUpdater.showLoadedTargets() },
        selectionProvider = { panelShown == PanelShown.LOADED },
      )
    )

    actionGroup.addSeparator()
    actionGroup.add(FilterActionGroup(listsUpdater.targetFilter))

    val actionToolbar = actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true)
    actionToolbar.targetComponent = this.component
    actionToolbar.setOrientation(SwingConstants.HORIZONTAL)
    this.toolbar = actionToolbar.component

    showCurrentPanel(listsUpdater)
  }

  private fun AnAction.shouldBeDisposedAfterReload(): Boolean {
    val notLoadedTargetsActionName = BspPluginBundle.message("widget.not.loaded.targets.tab.name")
    val loadedTargetsActionName = BspPluginBundle.message("widget.loaded.targets.tab.name")
    val restartActionName = BspPluginBundle.message("restart.action.text")

    return this.templateText == notLoadedTargetsActionName ||
      this.templateText == loadedTargetsActionName ||
      this.templateText == restartActionName ||
      this is FilterActionGroup
  }

  private fun ListsUpdater.showLoadedTargets() {
    panelShown = PanelShown.LOADED
    showCurrentPanel(this)
  }

  private fun ListsUpdater.showNotLoadedTargets() {
    panelShown = PanelShown.NOTLOADED
    showCurrentPanel(this)
  }

  private fun showCurrentPanel(listsUpdater: ListsUpdater) {
    when (panelShown) {
      PanelShown.LOADED -> listsUpdater.loadedTargetsPanel
      PanelShown.NOTLOADED -> listsUpdater.notLoadedTargetsPanel
    }.let { setToolWindowContent(it.wrappedInScrollPane()) }
  }

  private fun setToolWindowContent(component: JComponent) {
    this.setContent(component)
  }
}
