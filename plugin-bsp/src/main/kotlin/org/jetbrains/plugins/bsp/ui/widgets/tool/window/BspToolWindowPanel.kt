package org.jetbrains.plugins.bsp.ui.widgets.tool.window

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.services.BspConnectionService
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RestartAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.BspTargetTree
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.NotLoadedTargetsMouseListener
import javax.swing.JComponent
import javax.swing.SwingConstants

public class ListsUpdater(
  public val magicMetaModel: MagicMetaModel,
  private val toolName: String?
) {
  private val loadedBspTargetTree: BspTargetTree = BspTargetTree(BspPluginIcons.bsp)
  public val loadedTargetsTreeComponent: Tree = loadedBspTargetTree.treeComponent

  private val notLoadedBspTargetTree: BspTargetTree = BspTargetTree(BspPluginIcons.notLoadedTarget)
  public val notLoadedTargetsTreeComponent: Tree = notLoadedBspTargetTree.treeComponent

  init {
    updateModels()
  }

  public fun updateModels() {
    loadedBspTargetTree.generateTree(toolName, magicMetaModel.getAllLoadedTargets())
    notLoadedBspTargetTree.generateTree(toolName, magicMetaModel.getAllNotLoadedTargets())
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {

  public constructor(project: Project) : this() {

    val bspUtilService = BspUtilService.getInstance()
    val magicMetaModel = MagicMetaModelService.getInstance(project).magicMetaModel
    val bspConnectionService = BspConnectionService.getInstance(project)
    val actionManager = ActionManager.getInstance()
    val listsUpdater = ListsUpdater(magicMetaModel, bspConnectionService.toolName)

    listsUpdater.loadedTargetsTreeComponent.addMouseListener(LoadedTargetsMouseListener(listsUpdater))
    listsUpdater.notLoadedTargetsTreeComponent.addMouseListener(NotLoadedTargetsMouseListener(listsUpdater))

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

    if (!bspUtilService.loadedViaBspFile.contains(project.locationHash)) {
      actionGroup.add(RestartAction(restartActionName, BspPluginIcons.restart), Constraints(Anchor.AFTER, "Bsp.ReloadAction"))
    }

    actionGroup.addSeparator()

    actionGroup.add(object : AnAction({ notLoadedTargetsActionName }, BspPluginIcons.notLoadedTarget) {
      override fun actionPerformed(e: AnActionEvent) {
        setToolWindowContent(JBScrollPane(listsUpdater.notLoadedTargetsTreeComponent))
      }
    })
    actionGroup.add(object : AnAction({ loadedTargetsActionName }, BspPluginIcons.loadedTarget) {
      override fun actionPerformed(e: AnActionEvent) {
        setToolWindowContent(JBScrollPane(listsUpdater.loadedTargetsTreeComponent))
      }
    })

    val actionToolbar = actionManager.createActionToolbar("Bsp Toolbar", actionGroup, true)
    actionToolbar.targetComponent = this.component
    actionToolbar.setOrientation(SwingConstants.HORIZONTAL)
    this.toolbar = actionToolbar.component
  }

  public fun setToolWindowContent(component: JComponent) {
    this.setContent(component)
  }
}
