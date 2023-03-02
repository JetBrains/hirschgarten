package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.server.connection.BspConnectionService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RestartAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.NotLoadedTargetsMouseListener
import javax.swing.JComponent
import javax.swing.SwingConstants

private enum class PanelShown {
  LOADED,
  NOTLOADED,
  NONE
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

  init {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    loadedTargetsPanel =
      BspPanelComponent(BspPluginIcons.bsp, toolName ?: "", magicMetaModel.getAllLoadedTargets())
    loadedTargetsPanel.addMouseListener { LoadedTargetsMouseListener(it) }

    notLoadedTargetsPanel =
      BspPanelComponent(BspPluginIcons.bsp, toolName ?: "", magicMetaModel.getAllNotLoadedTargets())
    notLoadedTargetsPanel.addMouseListener { NotLoadedTargetsMouseListener(it) }
    magicMetaModel.registerTargetLoadListener { rerenderComponents() }
  }

  fun rerenderComponents() {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    loadedTargetsPanel = loadedTargetsPanel.createNewWithTargets(magicMetaModel.getAllLoadedTargets())
    notLoadedTargetsPanel = notLoadedTargetsPanel.createNewWithTargets(magicMetaModel.getAllNotLoadedTargets())
    targetPanelUpdater(this@ListsUpdater)
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {
  private var panelShown = PanelShown.NONE

  public constructor(project: Project) : this() {
    val actionManager = ActionManager.getInstance()
    val bspConnection = BspConnectionService.getInstance(project).value
    val listsUpdater = ListsUpdater(project, bspConnection.buildToolId, this::showCurrentPanel)

    val actionGroup = actionManager
      .getAction("Bsp.ActionsToolbar") as DefaultActionGroup

    val notLoadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.not.loaded.targets.tab.name")
    val loadedTargetsActionName = BspAllTargetsWidgetBundle.message("widget.loaded.targets.tab.name")
    val restartActionName = BspAllTargetsWidgetBundle.message("restart.action.text")

    actionGroup.childActionsOrStubs.iterator().forEach {
      if ((it.templateText == notLoadedTargetsActionName) || (it.templateText == loadedTargetsActionName) || (it.templateText == restartActionName)) {
        actionGroup.remove(it)
      }
    }

    actionGroup.add(
      RestartAction(),
      Constraints(Anchor.AFTER, "Bsp.ReloadAction")
    )

    actionGroup.addSeparator()

    actionGroup.add(object : AnAction({ notLoadedTargetsActionName }, BspPluginIcons.notLoadedTarget) {
      override fun actionPerformed(e: AnActionEvent) {
        showNotLoadedTargets(listsUpdater)
      }
    })
    actionGroup.add(object : AnAction({ loadedTargetsActionName }, BspPluginIcons.loadedTarget) {
      override fun actionPerformed(e: AnActionEvent) {
        showLoadedTargets(listsUpdater)
      }
    })

    val actionToolbar = actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true)
    actionToolbar.targetComponent = this.component
    actionToolbar.setOrientation(SwingConstants.HORIZONTAL)
    this.toolbar = actionToolbar.component
  }

  private fun showLoadedTargets(listsUpdater: ListsUpdater) {
    panelShown = PanelShown.LOADED
    showCurrentPanel(listsUpdater)
  }

  private fun showNotLoadedTargets(listsUpdater: ListsUpdater) {
    panelShown = PanelShown.NOTLOADED
    showCurrentPanel(listsUpdater)
  }

  private fun showCurrentPanel(listsUpdater: ListsUpdater) {
    when (panelShown) {
      PanelShown.LOADED -> listsUpdater.loadedTargetsPanel
      PanelShown.NOTLOADED -> listsUpdater.notLoadedTargetsPanel
      else -> null
    }?.let { setToolWindowContent(it.wrappedInScrollPane) }
  }

  private fun setToolWindowContent(component: JComponent) {
    this.setContent(component)
  }
}
