package org.jetbrains.bazel.ui.widgets.tool.window.utils

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.debug.actions.StarlarkDebugAction
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.runnerAction.BazelRunnerAction
import org.jetbrains.bazel.runnerAction.BuildTargetAction
import org.jetbrains.bazel.runnerAction.RunTargetAction
import org.jetbrains.bazel.runnerAction.RunWithCoverageAction
import org.jetbrains.bazel.runnerAction.RunWithLocalJvmRunnerAction
import org.jetbrains.bazel.runnerAction.TestTargetAction
import org.jetbrains.bazel.runnerAction.TestWithLocalJvmRunnerAction
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.action.ResyncTargetAction
import org.jetbrains.bazel.ui.widgets.BazelJumpToBuildFileAction
import org.jetbrains.bazel.ui.widgets.tool.window.components.BuildTargetContainer
import org.jetbrains.bazel.workspacemodel.entities.isJvmTarget
import org.jetbrains.bsp.protocol.BuildTarget
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
    val actionGroup =
      container.getSelectedBuildTarget()?.let { calculatePopupGroup(it) }
        ?: calculatePopupGroup(container.getSelectedBuildTargetsUnderDirectory())

    if (actionGroup != null) {
      ActionManager
        .getInstance()
        .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, actionGroup)
        .component
        .show(component, x, y)
    }
  }

  private fun calculatePopupGroup(target: BuildTarget): ActionGroup =
    DefaultActionGroup().apply {
      ResyncTargetAction.createIfEnabled(target.id)?.let { addAction(it) }
      addAction(container.copyTargetIdAction)
      addSeparator()
      if (target.capabilities.canCompile) {
        addAction(BuildTargetAction(target.id))
      }
      fillWithEligibleActions(project, target, false)
      add(BazelJumpToBuildFileAction(target))
      if (StarlarkDebugAction.isApplicableTo(target)) add(StarlarkDebugAction(target.id))
    }

  private fun calculatePopupGroup(targets: List<BuildTarget>): ActionGroup? {
    val testTargets = targets.filter { it.capabilities.canTest }
    return if (testTargets.isEmpty()) {
      null
    } else {
      DefaultActionGroup().apply {
        addAction(RunAllTestsActionInTargetTreeAction(testTargets, container.getSelectedComponentName()))
        addAction(RunAllTestsActionWithCoverageInTargetTreeAction(testTargets, container.getSelectedComponentName()))
      }
    }
  }

  private fun MouseEvent.isDoubleClick(): Boolean = this.mouseButton == MouseButton.Left && this.clickCount == 2

  private fun onDoubleClick() {
    container.getSelectedBuildTarget()?.also {
      when {
        it.capabilities.canTest -> TestTargetAction(targetInfos = listOf(it)).prepareAndPerform(project)
        it.capabilities.canRun -> RunTargetAction(targetInfo = it).prepareAndPerform(project)
        it.capabilities.canCompile -> BuildTargetAction.buildTarget(project, it.id)
      }
    }
  }
}

private fun BazelRunnerAction.prepareAndPerform(project: Project) {
  BazelCoroutineService.getInstance(project).start {
    doPerformAction(project)
  }
}

@Suppress("CognitiveComplexMethod")
fun DefaultActionGroup.fillWithEligibleActions(
  project: Project,
  target: BuildTarget,
  includeTargetNameInText: Boolean,
  singleTestFilter: String? = null,
): DefaultActionGroup {
  val canBeDebugged = RunHandlerProvider.getRunHandlerProvider(listOf(target), isDebug = true) != null
  if (target.capabilities.canRun) {
    addAction(RunTargetAction(target, includeTargetNameInText = includeTargetNameInText))
    if (canBeDebugged) {
      addAction(RunTargetAction(target, isDebugAction = true, includeTargetNameInText = includeTargetNameInText))
    }
  }

  if (target.capabilities.canTest) {
    addAction(TestTargetAction(listOf(target), includeTargetNameInText = includeTargetNameInText, singleTestFilter = singleTestFilter))
    if (canBeDebugged) {
      addAction(
        TestTargetAction(
          listOf(target),
          isDebugAction = true,
          includeTargetNameInText = includeTargetNameInText,
          singleTestFilter = singleTestFilter,
        ),
      )
    }
    addAction(RunWithCoverageAction(listOf(target), includeTargetNameInText = includeTargetNameInText, singleTestFilter = singleTestFilter))
  }

  if (project.bazelProjectSettings.enableLocalJvmActions && target.languageIds.isJvmTarget()) {
    if (target.capabilities.canRun) {
      addAction(RunWithLocalJvmRunnerAction(target, includeTargetNameInText = includeTargetNameInText))
      addAction(RunWithLocalJvmRunnerAction(target, isDebugMode = true, includeTargetNameInText = includeTargetNameInText))
    }
    if (target.capabilities.canTest) {
      addAction(TestWithLocalJvmRunnerAction(target, includeTargetNameInText = includeTargetNameInText))
      addAction(TestWithLocalJvmRunnerAction(target, isDebugMode = true, includeTargetNameInText = includeTargetNameInText))
    }
  }
  return this
}

internal class RunAllTestsActionInTargetTreeAction(private val targets: List<BuildTarget>, private val directoryName: String) :
  SuspendableAction(
    text = { BazelPluginBundle.message("action.run.all.tests") },
    icon = AllIcons.Actions.Execute,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    TestTargetAction(
      targets,
      text = {
        BazelPluginBundle.message("action.run.all.tests.under", directoryName)
      },
    ).actionPerformed(e)
  }
}

internal class RunAllTestsActionWithCoverageInTargetTreeAction(private val targets: List<BuildTarget>, private val directoryName: String) :
  SuspendableAction(
    text = { BazelPluginBundle.message("action.run.all.tests.with.coverage") },
    icon = AllIcons.General.RunWithCoverage,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    RunWithCoverageAction(
      targets,
      text = {
        BazelPluginBundle.message("action.run.all.tests.under.with.coverage", directoryName)
      },
    ).actionPerformed(e)
  }
}
