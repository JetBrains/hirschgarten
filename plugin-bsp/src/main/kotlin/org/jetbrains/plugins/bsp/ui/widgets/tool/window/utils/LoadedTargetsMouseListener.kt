package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.isJvmTarget
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.actions.target.BspRunnerAction
import org.jetbrains.plugins.bsp.ui.actions.target.BuildTargetAction
import org.jetbrains.plugins.bsp.ui.actions.target.RunTargetAction
import org.jetbrains.plugins.bsp.ui.actions.target.RunWithLocalJvmRunnerAction
import org.jetbrains.plugins.bsp.ui.actions.target.TestTargetAction
import org.jetbrains.plugins.bsp.ui.actions.target.TestWithLocalJvmRunnerAction
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandler
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetContainer
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.BuildTargetSearch
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent

public class LoadedTargetsMouseListener(private val container: BuildTargetContainer, private val project: Project) : PopupHandler() {
  override fun mouseClicked(mouseEvent: MouseEvent) {
    if (mouseEvent.isDoubleClick()) {
      onDoubleClick()
    } else {
      super.mouseClicked(mouseEvent)
    }
  }

  /**
   * Inherit from PopupHandler instead of MouseListener to be called in
   * [remote dev scenarios](https://code.jetbrains.team/p/ij/repositories/ultimate/files/ebcc1e5735999c995ba1dd00be8003b66d2e8309/remote-dev/rd-ui/src/com/jetbrains/rd/ui/bedsl/BeDslBehavior.kt?tab=source&line=98&lines-count=1)
   */
  override fun invokePopup(
    component: Component,
    x: Int,
    y: Int,
  ) {
    selectTargetIfSearchListIsDisplayed(Point(x, y))
    showPopup(component, x, y)
  }

  private fun selectTargetIfSearchListIsDisplayed(point: Point) {
    if (container is BuildTargetSearch) {
      container.selectAtLocationIfListDisplayed(point)
    }
  }

  private fun showPopup(
    component: Component,
    x: Int,
    y: Int,
  ) {
    val actionGroup = container.getSelectedBuildTarget()?.let { calculatePopupGroup(it) }
    if (actionGroup != null) {
      ActionManager
        .getInstance()
        .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, actionGroup)
        .component
        .show(component, x, y)
    }
  }

  private fun calculatePopupGroup(target: BuildTargetInfo): ActionGroup =
    DefaultActionGroup().apply {
      addAction(container.copyTargetIdAction)
      addSeparator()
      if (target.capabilities.canCompile) {
        addAction(BuildTargetAction(target.id))
      }
      fillWithEligibleActions(target, false)
      container.getTargetActions(project, target).map {
        addAction(it)
        addSeparator()
      }
    }

  private fun MouseEvent.isDoubleClick(): Boolean = this.mouseButton == MouseButton.Left && this.clickCount == 2

  private fun onDoubleClick() {
    container.getSelectedBuildTarget()?.also {
      when {
        it.capabilities.canTest -> TestTargetAction(targetInfo = it).prepareAndPerform(project)
        it.capabilities.canRun -> RunTargetAction(targetInfo = it).prepareAndPerform(project)
        it.capabilities.canCompile -> BuildTargetAction.buildTarget(project, it.id)
      }
    }
  }
}

private fun BspRunnerAction.prepareAndPerform(project: Project) {
  BspCoroutineService.getInstance(project).start {
    doPerformAction(project)
  }
}

@Suppress("CognitiveComplexMethod")
public fun DefaultActionGroup.fillWithEligibleActions(target: BuildTargetInfo, verboseText: Boolean): DefaultActionGroup {
  if (target.capabilities.canRun) {
    addAction(
      RunTargetAction(
        targetInfo = target,
        verboseText = verboseText,
      ),
    )
  }

  if (target.capabilities.canTest) {
    addAction(TestTargetAction(target, verboseText = verboseText))
  }

  if (target.capabilities.canDebug && BspRunHandler.getRunHandler(listOf(target)).canDebug(listOf(target))) {
    addAction(
      RunTargetAction(
        targetInfo = target,
        isDebugAction = true,
        verboseText = verboseText,
      ),
    )
  }

  if (target.languageIds.isJvmTarget()) {
    if (target.capabilities.canRun) {
      addAction(RunWithLocalJvmRunnerAction(target, verboseText = verboseText))
      if (target.capabilities.canDebug) {
        addAction(RunWithLocalJvmRunnerAction(target, isDebugMode = true, verboseText = verboseText))
      }
    }
    if (target.capabilities.canTest) {
      addAction(TestWithLocalJvmRunnerAction(target, verboseText = verboseText))
      if (target.capabilities.canDebug) {
        addAction(TestWithLocalJvmRunnerAction(target, isDebugMode = true, verboseText = verboseText))
      }
    }
  }
  return this
}
