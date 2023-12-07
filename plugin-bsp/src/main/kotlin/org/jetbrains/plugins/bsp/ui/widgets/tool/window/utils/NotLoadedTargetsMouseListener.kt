package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.actions.LoadTargetAction
import org.jetbrains.plugins.bsp.ui.actions.LoadTargetWithDependenciesAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetContainer
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetSearch
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

public class NotLoadedTargetsMouseListener(
  private val container: BuildTargetContainer,
  private val project: Project,
) : MouseListener {
  override fun mouseClicked(e: MouseEvent?) {
    e?.let { mouseClickedNotNull(it) }
  }

  private fun mouseClickedNotNull(mouseEvent: MouseEvent) {
    if (mouseEvent.mouseButton == MouseButton.Right) {
      selectTargetIfSearchListIsDisplayed(mouseEvent.point)
      showPopup(mouseEvent)
    } else if (mouseEvent.isDoubleClick()) {
      onDoubleClick()
    }
  }

  // https://youtrack.jetbrains.com/issue/BAZEL-522
  private fun selectTargetIfSearchListIsDisplayed(point: Point) {
    if (container is BuildTargetSearch) {
      container.selectAtLocationIfListDisplayed(point)
    }
  }

  private fun showPopup(mouseEvent: MouseEvent) {
    val actionGroup = calculatePopupGroup()
    if (actionGroup != null) {
      val context = DataManager.getInstance().getDataContext(mouseEvent.component)
      val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
      JBPopupFactory.getInstance()
        .createActionGroupPopup(null, actionGroup, context, mnemonics, true)
        .showInBestPositionFor(context)
    }
  }

  private fun calculatePopupGroup(): ActionGroup? {
    val target = container.getSelectedBuildTarget()

    return if (target != null) {
      val copyTargetIdAction = container.copyTargetIdAction
      val loadTargetAction = LoadTargetAction(
        targetId = target.id,
        text = { org.jetbrains.plugins.bsp.config.BspPluginBundle.message("widget.load.target.popup.message") },
      )
      val loadTargetWithDepsAction = LoadTargetWithDependenciesAction(
        targetId = target.id,
        text = {
          org.jetbrains.plugins.bsp.config.BspPluginBundle.message("widget.load.target.with.deps.popup.message")
        },
      )
      DefaultActionGroup().apply {
        addAction(copyTargetIdAction)
        addSeparator()
        addAction(loadTargetAction)
        addSeparator()
        addAction(loadTargetWithDepsAction)
      }
    } else null
  }

  private fun MouseEvent.isDoubleClick(): Boolean =
    this.mouseButton == MouseButton.Left && this.clickCount == 2

  private fun onDoubleClick() {
    container.getSelectedBuildTarget()?.let {
      BspCoroutineService.getInstance(project).start { LoadTargetAction.loadTarget(project, it.id) }
    }
  }

  override fun mousePressed(e: MouseEvent?) { // nothing to do
  }

  override fun mouseReleased(e: MouseEvent?) { // nothing to do
  }

  override fun mouseEntered(e: MouseEvent?) { // nothing to do
  }

  override fun mouseExited(e: MouseEvent?) { // nothing to do
  }
}
