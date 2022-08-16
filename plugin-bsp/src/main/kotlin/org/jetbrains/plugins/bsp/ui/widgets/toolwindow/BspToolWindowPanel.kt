package org.jetbrains.plugins.bsp.ui.widgets.toolwindow

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Anchor
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.DefaultListModel
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.ui.widgets.toolwindow.actions.RestartAction
import org.jetbrains.plugins.bsp.ui.widgets.toolwindow.all.targets.BspAllTargetsWidgetBundle

private class ListsUpdater(
  val magicMetaModel: MagicMetaModel,
) {

  private val loadedTargetsListModel = DefaultListModel<BuildTarget>()
  val loadedTargetsJbList = JBList(loadedTargetsListModel)

  private val notLoadedTargetsListModel = DefaultListModel<BuildTarget>()
  val notLoadedTargetsJbList = JBList(notLoadedTargetsListModel)

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

  fun updateModels() {
    loadedTargetsListModel.removeAllElements()
    loadedTargetsListModel.addAll(magicMetaModel.getAllLoadedTargets())

    notLoadedTargetsListModel.removeAllElements()
    notLoadedTargetsListModel.addAll(magicMetaModel.getAllNotLoadedTargets())
  }
}

private class LoadTargetAction(
  text: String,
  private val target: BuildTargetIdentifier,
  private val listsUpdater: ListsUpdater,
) : AnAction(text) {

  override fun actionPerformed(e: AnActionEvent) {
    listsUpdater.magicMetaModel.loadTarget(target)
    runWriteAction {
      listsUpdater.magicMetaModel.save()
    }
    listsUpdater.updateModels()
  }
}

private class NotLoadedTargetsListMouseListener(
  private val listsUpdater: ListsUpdater,
) : MouseListener {

  override fun mouseClicked(e: MouseEvent?) = mouseClickedNotNull(e!!)

  private fun mouseClickedNotNull(mouseEvent: MouseEvent) {
    updateSelectedIndex(mouseEvent)

    showPopupIfRightButtonClicked(mouseEvent)
  }

  private fun updateSelectedIndex(mouseEvent: MouseEvent) {
    listsUpdater.notLoadedTargetsJbList.selectedIndex =
      listsUpdater.notLoadedTargetsJbList.locationToIndex(mouseEvent.point)
  }

  private fun showPopupIfRightButtonClicked(mouseEvent: MouseEvent) {
    if (mouseEvent.mouseButton == MouseButton.Right) {
      showPopup(mouseEvent)
    }
  }

  private fun showPopup(mouseEvent: MouseEvent) {
    val actionGroup = calculatePopupGroup()
    val context = DataManager.getInstance().getDataContext(mouseEvent.component)
    val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS

    JBPopupFactory.getInstance()
      .createActionGroupPopup(null, actionGroup, context, mnemonics, true)
      .showInBestPositionFor(context)
  }

  private fun calculatePopupGroup(): ActionGroup {
    val group = DefaultActionGroup()

    val target = listsUpdater.notLoadedTargetsJbList.selectedValue.id
    val action = LoadTargetAction(
      BspAllTargetsWidgetBundle.message("widget.load.target.popup.message"),
      target,
      listsUpdater
    )
    group.addAction(action)

    return group
  }

  override fun mousePressed(e: MouseEvent?) {
    // nothing
  }

  override fun mouseReleased(e: MouseEvent?) {
    // nothing
  }

  override fun mouseEntered(e: MouseEvent?) {
    // listsUpdater.updateModels()
  }

  override fun mouseExited(e: MouseEvent?) {
    // nothing
  }
}

public class BspToolWindowPanel() : SimpleToolWindowPanel(true, true) {

  public constructor(project: Project) : this() {

    val bspUtilService = BspUtilService.getInstance()
    val magicMetaModel = MagicMetaModelService.getInstance(project).magicMetaModel
    val actionManager = ActionManager.getInstance()
    val listsUpdater = ListsUpdater(magicMetaModel)

    listsUpdater.notLoadedTargetsJbList.addMouseListener(NotLoadedTargetsListMouseListener(listsUpdater))

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
