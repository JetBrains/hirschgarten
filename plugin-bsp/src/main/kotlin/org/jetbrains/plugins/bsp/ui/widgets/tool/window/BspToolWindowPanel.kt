package org.jetbrains.plugins.bsp.ui.widgets.tool.window

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Anchor
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.ProjectPropertiesService
import org.jetbrains.plugins.bsp.extension.points.BspConnectionDetailsGeneratorExtension
import org.jetbrains.plugins.bsp.protocol.connection.BspConnectionDetailsGeneratorProvider
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RestartAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.BspTargetTree
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.NotLoadedTargetsMouseListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

private enum class PanelShown {
  LOADED,
  NOTLOADED,
  NONE
}

private class ListsUpdater(
  private val project: Project,
  private val toolName: String?,
  private val targetPanelUpdater: (ListsUpdater) -> Unit
) {
  private val loadedBspTargetTree: BspTargetTree = BspTargetTree(BspPluginIcons.bsp)
  fun getLoadedTargetsPanelComponent(): JPanel = loadedBspTargetTree.panelComponent

  private val notLoadedBspTargetTree: BspTargetTree = BspTargetTree(BspPluginIcons.notLoadedTarget)
  fun getNotLoadedTargetsPanelComponent(): JPanel = notLoadedBspTargetTree.panelComponent

  init {
    loadedBspTargetTree.addMouseListeners { component ->
      LoadedTargetsMouseListener(component)
    }
    notLoadedBspTargetTree.addMouseListeners { component ->
      NotLoadedTargetsMouseListener(component)
    }
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    magicMetaModel.registerTargetLoadListener { rerenderComponents() }
    rerenderComponents()
  }

  fun rerenderComponents() {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    loadedBspTargetTree.generateTree(toolName, magicMetaModel.getAllLoadedTargets())
    notLoadedBspTargetTree.generateTree(toolName, magicMetaModel.getAllNotLoadedTargets())
    loadedBspTargetTree.regenerateComponents()
    notLoadedBspTargetTree.regenerateComponents()
    targetPanelUpdater(this@ListsUpdater)
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {
  private var panelShown = PanelShown.NONE

  public constructor(project: Project) : this() {
    val actionManager = ActionManager.getInstance()
    val projectProperties = ProjectPropertiesService.getInstance(project).value
    val g = BspConnectionDetailsGeneratorProvider(
      projectProperties.projectRootDir,
      BspConnectionDetailsGeneratorExtension.extensions()
    )
    val listsUpdater = ListsUpdater(project, g.firstGeneratorTEMPORARY(), this::showCurrentPanel)

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
      PanelShown.LOADED -> listsUpdater.getLoadedTargetsPanelComponent()
      PanelShown.NOTLOADED -> listsUpdater.getNotLoadedTargetsPanelComponent()
      else -> null
    }?.let { setToolWindowContent(JBScrollPane(it)) }
  }

  private fun setToolWindowContent(component: JComponent) {
    this.setContent(component)
  }
}
