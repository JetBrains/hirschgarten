package org.jetbrains.plugins.bsp.ui.widgets.tool.window

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Anchor
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RestartAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.LoadedTargetsMouseListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.NotLoadedTargetsListMouseListener
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

public class ListsUpdater(
  public val magicMetaModel: MagicMetaModel,
) {

  private val loadedTargetsListModel = DefaultListModel<BuildTarget>()
  public val loadedTargetsJbList: JBList<BuildTarget> = JBList(loadedTargetsListModel)

  private val notLoadedTargetsListModel = DefaultListModel<BuildTarget>()
  public val notLoadedTargetsJbList: JBList<BuildTarget> = JBList(notLoadedTargetsListModel)

  init {
    loadedTargetsJbList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    loadedTargetsJbList.installCellRenderer {
      JBLabel(
        it.displayName ?: it.id.uri,
        BspPluginIcons.bsp,
        SwingConstants.LEFT
      )
    }

    notLoadedTargetsJbList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    notLoadedTargetsJbList.installCellRenderer {
      JBLabel(
        it.displayName ?: it.id.uri,
        BspPluginIcons.notLoadedTarget,
        SwingConstants.LEFT
      )
    }

    updateModels()
  }

  public fun updateModels() {
    loadedTargetsListModel.removeAllElements()
    loadedTargetsListModel.addAll(magicMetaModel.getAllLoadedTargets())

    notLoadedTargetsListModel.removeAllElements()
    notLoadedTargetsListModel.addAll(magicMetaModel.getAllNotLoadedTargets())
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {

  public constructor(project: Project) : this() {

    val bspUtilService = BspUtilService.getInstance()
    val magicMetaModel = MagicMetaModelService.getInstance(project).magicMetaModel
    val actionManager = ActionManager.getInstance()
    val listsUpdater = ListsUpdater(magicMetaModel)

    listsUpdater.notLoadedTargetsJbList.addMouseListener(NotLoadedTargetsListMouseListener(listsUpdater))
    listsUpdater.loadedTargetsJbList.addMouseListener(LoadedTargetsMouseListener(listsUpdater))

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
        setToolWindowContent(JBScrollPane(listsUpdater.notLoadedTargetsJbList))
      }
    })
    actionGroup.add(object : AnAction({ loadedTargetsActionName }, BspPluginIcons.loadedTarget) {
      override fun actionPerformed(e: AnActionEvent) {
        setToolWindowContent(JBScrollPane(listsUpdater.loadedTargetsJbList))
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
