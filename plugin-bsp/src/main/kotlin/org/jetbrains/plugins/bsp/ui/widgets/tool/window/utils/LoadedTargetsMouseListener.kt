package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesJava
import org.jetbrains.magicmetamodel.impl.workspacemodel.includesKotlin
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.AbstractActionWithTarget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.BuildTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.DebugWithLocalJvmRunnerAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RunTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.RunWithLocalJvmRunnerAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.SideMenuRunnerAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.TestTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetContainer
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetSearch
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

public class LoadedTargetsMouseListener(
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
      val actions = mutableListOf<AnAction>()
      if (target.capabilities.canCompile) {
        actions.addAction(BuildTargetAction::class.java)
      }
      if (target.capabilities.canRun) {
        actions.addAction(RunTargetAction::class.java)
        if (target.isJvmTarget()) {
          actions.addAction(RunWithLocalJvmRunnerAction::class.java)
          actions.addAction(DebugWithLocalJvmRunnerAction::class.java)
        }
      }
      if (target.capabilities.canTest) {
        actions.addAction(TestTargetAction::class.java)
      }
      DefaultActionGroup().apply {
        addAction(container.copyTargetIdAction)
        addSeparator()
        addAll(actions)
      }
    } else null
  }

  private fun MutableList<AnAction>.addAction(
    actionClass: Class<out AbstractActionWithTarget>,
  ): AbstractActionWithTarget =
    actions.getOrPut(actionClass) {
      actionClass.constructors.first { it.parameterCount == 0 }.newInstance() as AbstractActionWithTarget
    }.also {
      it.target = container.getSelectedBuildTarget()?.id
      add(it)
    }

  private fun BuildTargetInfo.isJvmTarget(): Boolean = with(languageIds) {
    includesJava() or includesKotlin()
  }

  private fun MouseEvent.isDoubleClick(): Boolean =
    this.mouseButton == MouseButton.Left && this.clickCount == 2

  private fun onDoubleClick() {
    container.getSelectedBuildTarget()?.also {
      when {
        it.capabilities.canTest -> TestTargetAction().prepareAndPerform(project, it.id)
        it.capabilities.canRun -> RunTargetAction().prepareAndPerform(project, it.id)
        it.capabilities.canCompile -> BuildTargetAction.buildTarget(project, it.id)
      }
    }
  }

  override fun mousePressed(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseReleased(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseEntered(e: MouseEvent?) { /* nothing to do */ }

  override fun mouseExited(e: MouseEvent?) { /* nothing to do */ }

  private companion object {
    val actions = HashMap<Class<out AbstractActionWithTarget>, AbstractActionWithTarget>()
  }
}

private fun SideMenuRunnerAction.prepareAndPerform(project: Project, targetId: BuildTargetId) {
  this.target = targetId
  doPerformAction(project, targetId)
}
