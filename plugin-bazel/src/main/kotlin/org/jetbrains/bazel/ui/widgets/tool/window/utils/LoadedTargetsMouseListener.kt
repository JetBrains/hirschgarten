package org.jetbrains.bazel.ui.widgets.tool.window.utils

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import org.jetbrains.bazel.coroutines.BspCoroutineService
import org.jetbrains.bazel.debug.actions.StarlarkDebugAction
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.runnerAction.BspRunnerAction
import org.jetbrains.bazel.runnerAction.BuildTargetAction
import org.jetbrains.bazel.runnerAction.RunTargetAction
import org.jetbrains.bazel.runnerAction.RunWithLocalJvmRunnerAction
import org.jetbrains.bazel.runnerAction.TestTargetAction
import org.jetbrains.bazel.runnerAction.TestWithLocalJvmRunnerAction
import org.jetbrains.bazel.sync.action.ResyncTargetAction
import org.jetbrains.bazel.ui.widgets.BazelBspJumpToBuildFileAction
import org.jetbrains.bazel.ui.widgets.tool.window.components.BuildTargetContainer
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.isJvmTarget
import java.awt.Component
import java.awt.event.MouseEvent

class LoadedTargetsMouseListener(private val container: BuildTargetContainer, private val project: Project) : PopupHandler() {
  override fun mouseClicked(mouseEvent: MouseEvent) {
    if (mouseEvent.isDoubleClick() && container.isPointSelectable(mouseEvent.point)) {
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
    showPopup(component, x, y)
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
      ResyncTargetAction.createIfEnabled(target.id)?.let { addAction(it) }
      addAction(container.copyTargetIdAction)
      addSeparator()
      if (target.capabilities.canCompile) {
        addAction(BuildTargetAction(target.id))
      }
      fillWithEligibleActions(project, target, false)
      add(BazelBspJumpToBuildFileAction(target))
      if (StarlarkDebugAction.isApplicableTo(target)) add(StarlarkDebugAction(target.id))
    }

  private fun MouseEvent.isDoubleClick(): Boolean = this.mouseButton == MouseButton.Left && this.clickCount == 2

  private fun onDoubleClick() {
    container.getSelectedBuildTarget()?.also {
      when {
        it.capabilities.canTest -> TestTargetAction(project = project, targetInfos = listOf(it)).prepareAndPerform(project)
        it.capabilities.canRun -> RunTargetAction(project = project, targetInfo = it).prepareAndPerform(project)
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
fun DefaultActionGroup.fillWithEligibleActions(
  project: Project,
  target: BuildTargetInfo,
  verboseText: Boolean,
  singleTestFilter: String? = null,
): DefaultActionGroup {
  val canBeDebugged = RunHandlerProvider.getRunHandlerProvider(listOf(target), isDebug = true) != null
  if (target.capabilities.canRun) {
    addAction(RunTargetAction(target, verboseText = verboseText, project = project))
    if (canBeDebugged) {
      addAction(RunTargetAction(target, isDebugAction = true, verboseText = verboseText, project = project))
    }
  }

  if (target.capabilities.canTest) {
    addAction(TestTargetAction(listOf(target), verboseText = verboseText, project = project, singleTestFilter = singleTestFilter))
    if (canBeDebugged) {
      addAction(
        TestTargetAction(
          listOf(target),
          isDebugAction = true,
          verboseText = verboseText,
          project = project,
          singleTestFilter = singleTestFilter,
        ),
      )
    }
  }

  if (target.languageIds.isJvmTarget()) {
    if (target.capabilities.canRun) {
      addAction(RunWithLocalJvmRunnerAction(target, verboseText = verboseText))
      addAction(RunWithLocalJvmRunnerAction(target, isDebugMode = true, verboseText = verboseText))
    }
    if (target.capabilities.canTest) {
      addAction(TestWithLocalJvmRunnerAction(target, verboseText = verboseText))
      addAction(TestWithLocalJvmRunnerAction(target, isDebugMode = true, verboseText = verboseText))
    }
  }
  return this
}
