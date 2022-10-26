package org.jetbrains.plugins.bsp.ui.widgets.tool.window

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Anchor
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBScrollPane
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.plugins.bsp.config.BspPluginIcons
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

public class ListsUpdater(
  public val magicMetaModel: MagicMetaModel,
  private val toolName: String?
) {
  private val loadedBspTargetTree: BspTargetTree = BspTargetTree(BspPluginIcons.bsp)
  public val loadedTargetsPanelComponent: JPanel = loadedBspTargetTree.panelComponent

  private val notLoadedBspTargetTree: BspTargetTree = BspTargetTree(BspPluginIcons.notLoadedTarget)
  public val notLoadedTargetsPanelComponent: JPanel = notLoadedBspTargetTree.panelComponent

  init {
    updateModels()
    loadedBspTargetTree.addMouseListeners { component ->
      LoadedTargetsMouseListener(component)
    }
    notLoadedBspTargetTree.addMouseListeners { component ->
      NotLoadedTargetsMouseListener(this@ListsUpdater, component)
    }
  }

  public fun updateModels() {
    loadedBspTargetTree.generateTree(toolName, magicMetaModel.getAllLoadedTargets())
    notLoadedBspTargetTree.generateTree(toolName, magicMetaModel.getAllNotLoadedTargets())
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {

  public constructor(project: Project) : this() {
    val magicMetaModel = MagicMetaModelService.getInstance(project).magicMetaModel
    val actionManager = ActionManager.getInstance()
    val g = BspConnectionDetailsGeneratorProvider(
      project.guessProjectDir()!!,
      BspConnectionDetailsGeneratorExtension.extensions()
    )
    val listsUpdater = ListsUpdater(magicMetaModel, g.firstGeneratorTEMPORARY())

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
        setToolWindowContent(JBScrollPane(listsUpdater.notLoadedTargetsPanelComponent))
      }
    })
    actionGroup.add(object : AnAction({ loadedTargetsActionName }, BspPluginIcons.loadedTarget) {
      override fun actionPerformed(e: AnActionEvent) {
        setToolWindowContent(JBScrollPane(listsUpdater.loadedTargetsPanelComponent))
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
