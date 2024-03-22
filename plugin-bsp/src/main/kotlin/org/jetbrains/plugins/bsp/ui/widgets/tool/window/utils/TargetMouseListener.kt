package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.BuildToolWindowTargetActionProviderExtension
import org.jetbrains.plugins.bsp.extension.points.withBuildToolId
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetContainer
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetSearch
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

public class TargetMouseListener(
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

  // Fixes https://youtrack.jetbrains.com/issue/BAZEL-522
  private fun selectTargetIfSearchListIsDisplayed(point: Point) {
    if (container is BuildTargetSearch) {
      container.selectAtLocationIfListDisplayed(point)
    }
  }

  private fun showPopup(mouseEvent: MouseEvent) {
    val actionGroup = container.getSelectedNode()?.let {
      if (it is TargetNode.Target) calculateTargetPopupGroup(it)
      else null // if target tree directory context actions are desired in the future, they should be obtained here
    }
    if (actionGroup != null) {
      val context = DataManager.getInstance().getDataContext(mouseEvent.component)
      val mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS
      JBPopupFactory.getInstance()
        .createActionGroupPopup(null, actionGroup, context, mnemonics, true)
        .showInBestPositionFor(context)
    }
  }

  private fun calculateTargetPopupGroup(node: TargetNode.Target): ActionGroup {
    val copyAction = container.copyTargetIdAction
    val standardActions = TargetActions.getStandardContextActions(node, inGutter = false)
    val buildToolActions = node.getBuildToolSpecificActions(container)

    return DefaultActionGroup().apply {
      add(copyAction)
      if (standardActions.isNotEmpty()) {
        addSeparator()
        addAll(standardActions)
      }
      if (buildToolActions.isNotEmpty()) {
        addSeparator()
        addAll(buildToolActions)
      }
    }
  }

  private fun TargetNode.getBuildToolSpecificActions(container: BuildTargetContainer): List<AnAction> =
    (this as? TargetNode.ValidTarget)?.let {
      BuildToolWindowTargetActionProviderExtension.ep.withBuildToolId(project.buildToolId)
        ?.getTargetActions(container.getComponent(), project, this.target)
    } ?: emptyList()

  private fun MouseEvent.isDoubleClick(): Boolean =
    this.mouseButton == MouseButton.Left && this.clickCount == 2

  private fun onDoubleClick() {
    container.getSelectedNode()?.let {
      TargetActions.performDefaultAction(it, project)
    }
  }

  override fun mousePressed(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseReleased(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseEntered(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseExited(e: MouseEvent?) { /* nothing to do */ }
}
