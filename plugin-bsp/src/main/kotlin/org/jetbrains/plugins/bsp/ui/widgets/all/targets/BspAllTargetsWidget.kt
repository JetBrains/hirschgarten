package org.jetbrains.plugins.bsp.ui.widgets.all.targets

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.content.impl.ContentImpl
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.services.MagicMetaModelService
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants

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
        BspPluginIcons.bsp,
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
    listsUpdater.notLoadedTargetsJbList.selectedIndex = listsUpdater.notLoadedTargetsJbList.locationToIndex(mouseEvent.point)
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
    listsUpdater.updateModels()
  }

  override fun mouseExited(e: MouseEvent?) {
    // nothing
  }
}

public class BspAllTargetsWidgetFactory : ToolWindowFactory {

  override fun shouldBeAvailable(project: Project): Boolean =
    true

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val magicMetaModel = MagicMetaModelService.getInstance(project).magicMetaModel

    toolWindow.title = BspAllTargetsWidgetBundle.message("widget.title")

    val listsUpdater = ListsUpdater(magicMetaModel)

    val loadedTargetsTab = ContentImpl(
      listsUpdater.loadedTargetsJbList,
      BspAllTargetsWidgetBundle.message("widget.loaded.targets.tab.name"),
      true
    )
    toolWindow.contentManager.addContent(loadedTargetsTab)

    val notLoadedTargetsTab = ContentImpl(
      listsUpdater.notLoadedTargetsJbList,
      BspAllTargetsWidgetBundle.message("widget.not.loaded.targets.tab.name"),
      true
    )
    listsUpdater.notLoadedTargetsJbList.addMouseListener(NotLoadedTargetsListMouseListener(listsUpdater))
    toolWindow.contentManager.addContent(notLoadedTargetsTab)
  }
}
