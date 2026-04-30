package org.jetbrains.bazel.ui.widgets.tool.window.utils

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.Executor
import com.intellij.execution.actions.RunConfigurationsComboBoxAction
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.ui.PopupHandler
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.debug.actions.StarlarkDebugAction
import org.jetbrains.bazel.run.synthetic.SyntheticRunTargetUtils
import org.jetbrains.bazel.runnerAction.BazelRunnerAction
import org.jetbrains.bazel.runnerAction.BuildTargetAction
import org.jetbrains.bazel.runnerAction.RunTargetAction
import org.jetbrains.bazel.runnerAction.TestTargetAction
import org.jetbrains.bazel.runnerAction.getTestExecutors
import org.jetbrains.bazel.sync.action.ResyncTargetAction
import org.jetbrains.bazel.ui.widgets.BazelJumpToBuildFileAction
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.ExecutableTarget
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent

internal abstract class LoadedTargetsMouseListener(private val project: Project) : PopupHandler() {
  abstract fun isPointSelectable(point: Point): Boolean

  abstract fun getSelectedBuildTarget(): BuildTarget?

  abstract fun getSelectedBuildTargetsUnderDirectory(): List<BuildTarget>

  abstract val copyTargetIdAction: CopyTargetIdAction

  abstract val bazelJumpToBuildFileAction: BazelJumpToBuildFileAction

  abstract fun getSelectedComponentName(): String

  override fun mouseClicked(mouseEvent: MouseEvent) {
    if (mouseEvent.isDoubleClick() && isPointSelectable(mouseEvent.point)) {
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
      getSelectedBuildTarget()?.let { calculatePopupGroup(it) }
        ?: calculatePopupGroup(getSelectedBuildTargetsUnderDirectory())

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
      addAction(copyTargetIdAction)
      addSeparator()
      addAction(BuildTargetAction(target.id))
      fillWithEligibleActions(project, target)
      addAction(bazelJumpToBuildFileAction)
      add(StarlarkDebugAction(target.id))
    }

  private fun calculatePopupGroup(targets: List<BuildTarget>): ActionGroup? {
    val testTargets = targets.filter { it.kind.ruleType == RuleType.TEST }
    if (testTargets.isEmpty()) {
      return null
    }
    val directoryName = getSelectedComponentName()
    val group = DefaultActionGroup()
    val configurationName = ExecutionBundle.message("test.in.scope.presentable.text", directoryName)
    for (executor in getTestExecutors()) {
      group.addAction(
        TestTargetAction(
          project,
          targets,
          executor = executor,
          configurationName = configurationName,
        ),
      )
    }
    return group
  }

  private fun MouseEvent.isDoubleClick(): Boolean = this.mouseButton == MouseButton.Left && this.clickCount == 2

  private fun onDoubleClick() {
    getSelectedBuildTarget()?.also {
      when {
        it.kind.ruleType == RuleType.TEST -> TestTargetAction(project, target = it).prepareAndPerform(project)
        it.kind.ruleType == RuleType.BINARY -> RunTargetAction(project, target = it).prepareAndPerform(project)
        else -> BuildTargetAction.buildTarget(project, it.id)
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
internal fun DefaultActionGroup.fillWithEligibleActions(
  project: Project,
  target: ExecutableTarget,
  singleTestFilter: String? = null,
  testExecutableArguments: List<String> = emptyList(),
  callerPsiElement: PsiElement? = null,
): DefaultActionGroup {
  val kind = target.kind

  val supportedExecutors = getSupportedExecutors(project, target)

  if (kind.ruleType == RuleType.BINARY) {
    for (executor in supportedExecutors) {
      addAction(RunTargetAction(project, target, executor))
    }
  }

  if (kind.ruleType == RuleType.TEST) {
    for (executor in supportedExecutors) {
      addAction(
        TestTargetAction(
          project,
          target,
          executor = executor,
          singleTestFilter = singleTestFilter,
          testExecutableArguments = testExecutableArguments,
        ),
      )
    }
  }

  if (BazelFeatureFlags.syntheticRunEnable && target.kind.ruleType == RuleType.LIBRARY) {
    if (callerPsiElement != null) {
      SyntheticRunTargetUtils.addSyntheticRunActions(this, project, target, callerPsiElement)
    }
  }

  for (provider in LoadedTargetActionsProvider.ep.extensionList) {
    provider.onActionsInit(
      project = project,
      group = this,
      target = target,
      callerPsiElement = callerPsiElement,
    )
  }

  return this
}

internal fun getSupportedExecutors(project: Project, target: ExecutableTarget): List<Executor> {
  if (!target.kind.isExecutable) return emptyList()
  val runConfiguration = RunTargetAction(project, target).createRunConfiguration()
  val supportedExecutors = mutableListOf<Executor>()
  RunConfigurationsComboBoxAction.forAllExecutors { executor ->
    if (ProgramRunner.getRunner(executor.id, runConfiguration.configuration) != null) {
      supportedExecutors.add(executor)
    }
  }
  return supportedExecutors
}
