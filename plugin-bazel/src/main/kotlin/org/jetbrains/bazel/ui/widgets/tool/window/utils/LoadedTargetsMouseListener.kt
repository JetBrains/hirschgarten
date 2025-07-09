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
import com.intellij.psi.PsiElement
import com.intellij.ui.PopupHandler
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.commons.RuleType
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
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.sync.action.ResyncTargetAction
import org.jetbrains.bazel.ui.widgets.BazelJumpToBuildFileAction
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.bsp.protocol.BuildTarget
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent

abstract class LoadedTargetsMouseListener(private val project: Project) : PopupHandler() {
  abstract fun isPointSelectable(point: Point): Boolean

  abstract fun getSelectedBuildTarget(): BuildTarget?

  abstract fun getSelectedBuildTargetsUnderDirectory(): List<BuildTarget>

  abstract val copyTargetIdAction: CopyTargetIdAction

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
      if (!target.noBuild) {
        addAction(BuildTargetAction(target.id))
      }
      fillWithEligibleActions(project, target, false)
      add(BazelJumpToBuildFileAction(target.id))
      if (StarlarkDebugAction.isApplicableTo(target)) add(StarlarkDebugAction(target.id))
    }

  private fun calculatePopupGroup(targets: List<BuildTarget>): ActionGroup? {
    val testTargets = targets.filter { it.kind.ruleType == RuleType.TEST }
    return if (testTargets.isEmpty()) {
      null
    } else {
      DefaultActionGroup().apply {
        addAction(RunAllTestsActionInTargetTreeAction(testTargets, getSelectedComponentName()))
        addAction(RunAllTestsActionWithCoverageInTargetTreeAction(testTargets, getSelectedComponentName()))
      }
    }
  }

  private fun MouseEvent.isDoubleClick(): Boolean = this.mouseButton == MouseButton.Left && this.clickCount == 2

  private fun onDoubleClick() {
    getSelectedBuildTarget()?.also {
      when {
        it.kind.ruleType == RuleType.TEST -> TestTargetAction(project = project, targetInfos = listOf(it)).prepareAndPerform(project)
        it.kind.ruleType == RuleType.BINARY -> RunTargetAction(project, targetInfo = it).prepareAndPerform(project)
        !it.noBuild -> BuildTargetAction.buildTarget(project, it.id)
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
  callerPsiElement: PsiElement? = null,
): DefaultActionGroup {
  val canBeDebugged = RunHandlerProvider.getRunHandlerProvider(listOf(target), isDebug = true) != null
  if (target.kind.ruleType == RuleType.BINARY) {
    addAction(RunTargetAction(project, target, includeTargetNameInText = includeTargetNameInText))
    if (canBeDebugged) {
      addAction(RunTargetAction(project, target, isDebugAction = true, includeTargetNameInText = includeTargetNameInText))
    }
  }

  if (target.kind.ruleType == RuleType.TEST) {
    addAction(
      TestTargetAction(
        project,
        listOf(target),
        includeTargetNameInText = includeTargetNameInText,
        singleTestFilter = singleTestFilter,
      ),
    )
    if (canBeDebugged) {
      addAction(
        TestTargetAction(
          project,
          listOf(target),
          isDebugAction = true,
          includeTargetNameInText = includeTargetNameInText,
          singleTestFilter = singleTestFilter,
        ),
      )
    }
    addAction(
      RunWithCoverageAction(
        project,
        listOf(target),
        includeTargetNameInText = includeTargetNameInText,
        singleTestFilter = singleTestFilter,
      ),
    )
  }

  if (project.bazelJVMProjectSettings.enableLocalJvmActions && target.kind.isJvmTarget()) {
    if (target.kind.ruleType == RuleType.BINARY) {
      addAction(RunWithLocalJvmRunnerAction(project, target, includeTargetNameInText = includeTargetNameInText))
      addAction(RunWithLocalJvmRunnerAction(project, target, isDebugMode = true, includeTargetNameInText = includeTargetNameInText))
    }
    if (target.kind.ruleType == RuleType.TEST) {
      if (callerPsiElement != null) { // called from gutter
        addLocalJvmTestActions(project, target, includeTargetNameInText, callerPsiElement)
      } else if (!project.bazelJVMProjectSettings.useIntellijTestRunner) { // called from target tree widget
        addLocalJvmTestActions(project, target, includeTargetNameInText, null)
      }
    }
  }
  return this
}

private fun DefaultActionGroup.addLocalJvmTestActions(
  project: Project,
  target: BuildTarget,
  includeTargetNameInText: Boolean,
  callerPsiElement: PsiElement?,
) {
  addAction(
    TestWithLocalJvmRunnerAction(project, target, includeTargetNameInText = includeTargetNameInText, callerPsiElement = callerPsiElement),
  )
  addAction(
    TestWithLocalJvmRunnerAction(
      project,
      target,
      isDebugMode = true,
      includeTargetNameInText = includeTargetNameInText,
      callerPsiElement = callerPsiElement,
    ),
  )
}

internal class RunAllTestsActionInTargetTreeAction(private val targets: List<BuildTarget>, private val directoryName: String) :
  SuspendableAction(
    text = { BazelPluginBundle.message("action.run.all.tests") },
    icon = AllIcons.Actions.Execute,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    TestTargetAction(
      project,
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
      project,
      targets,
      text = {
        BazelPluginBundle.message("action.run.all.tests.under.with.coverage", directoryName)
      },
    ).actionPerformed(e)
  }
}
