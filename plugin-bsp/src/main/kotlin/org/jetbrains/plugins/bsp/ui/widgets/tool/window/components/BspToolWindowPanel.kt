package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RestartAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.StickyTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.FilterActionGroup
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.NotLoadedTargetsMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter.TargetFilter
import javax.swing.JComponent
import javax.swing.SwingConstants

private enum class PanelShown {
  LOADED,
  NOTLOADED
}

private class ListsUpdater(
  private val project: Project,
  toolName: String?,
  private val targetPanelUpdater: (ListsUpdater) -> Unit
) {
  var loadedTargetsPanel: BspPanelComponent
    private set
  var notLoadedTargetsPanel: BspPanelComponent
    private set
  val targetFilter = TargetFilter(::rerenderComponents)

  init {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    loadedTargetsPanel =
      BspPanelComponent(BspPluginIcons.bsp, toolName ?: "", magicMetaModel.getAllLoadedTargets())
    loadedTargetsPanel.addMouseListener { LoadedTargetsMouseListener(it, project) }

    notLoadedTargetsPanel =
      BspPanelComponent(BspPluginIcons.bsp, toolName ?: "", magicMetaModel.getAllNotLoadedTargets())
    notLoadedTargetsPanel.addMouseListener { NotLoadedTargetsMouseListener(it, project) }
    magicMetaModel.registerTargetLoadListener { rerenderComponents() }
  }

  fun rerenderComponents() {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    loadedTargetsPanel = loadedTargetsPanel.createNewWithTargets(targetFilter.getMatchingLoadedTargets(magicMetaModel))
    notLoadedTargetsPanel = notLoadedTargetsPanel.createNewWithTargets(targetFilter.getMatchingNotLoadedTargets(magicMetaModel))
    targetPanelUpdater(this@ListsUpdater)
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {
  private var panelShown = PanelShown.LOADED

  public constructor(project: Project) : this() {
    val actionManager = ActionManager.getInstance()
    val bspConnection = BspConnectionService.getInstance(project).value
    val listsUpdater = ListsUpdater(project, bspConnection!!.buildToolId, this::showCurrentPanel)

    val actionGroup = actionManager
      .getAction("Bsp.ActionsToolbar") as DefaultActionGroup

    val notLoadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.not.loaded.targets.tab.name")
    val loadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.loaded.targets.tab.name")

    actionGroup.childActionsOrStubs.iterator().forEach {
      if ( it.shouldBeDisposedAfterReload() ) {
        actionGroup.remove(it)
      }
    }

    actionGroup.add(
      RestartAction(),
      Constraints(Anchor.AFTER, "Bsp.ReloadAction")
    )

    actionGroup.addSeparator()

    actionGroup.add(StickyTargetAction(
      hintText = notLoadedTargetsActionName,
      icon = BspPluginIcons.notLoadedTarget,
      onPerform = { listsUpdater.showNotLoadedTargets() },
      selectionProvider = { panelShown == PanelShown.NOTLOADED }
    ))
    actionGroup.add(StickyTargetAction(
      hintText = loadedTargetsActionName,
      icon = BspPluginIcons.loadedTarget,
      onPerform = { listsUpdater.showLoadedTargets() },
      selectionProvider = { panelShown == PanelShown.LOADED }
    ))

    actionGroup.addSeparator()
    actionGroup.add(FilterActionGroup(listsUpdater.targetFilter))

    val actionToolbar = actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true)
    actionToolbar.targetComponent = this.component
    actionToolbar.setOrientation(SwingConstants.HORIZONTAL)
    this.toolbar = actionToolbar.component

    showCurrentPanel(listsUpdater)
  }

  private fun AnAction.shouldBeDisposedAfterReload(): Boolean {
    val notLoadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.not.loaded.targets.tab.name")
    val loadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.loaded.targets.tab.name")
    val restartActionName = BspAllTargetsWidgetBundle.message("restart.action.text")

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
    }.let { setToolWindowContent(it.wrappedInScrollPane) }
  }

  private fun setToolWindowContent(component: JComponent) {
    this.setContent(component)
  }
}
