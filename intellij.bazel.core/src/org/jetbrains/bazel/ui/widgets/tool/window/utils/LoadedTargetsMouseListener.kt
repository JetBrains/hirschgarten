package org.jetbrains.bazel.ui.widgets.tool.window.utils

import com.intellij.codeInsight.hints.presentation.MouseButton
import com.intellij.codeInsight.hints.presentation.mouseButton
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.RunManagerEx
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.PreferredProducerFind
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.ui.PopupHandler
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.debug.actions.StarlarkDebugAction
import org.jetbrains.bazel.runnerAction.BuildTargetAction
import org.jetbrains.bazel.sync.action.ResyncTargetAction
import org.jetbrains.bazel.ui.gutters.BazelRunLocation
import org.jetbrains.bazel.ui.gutters.getExecutorActions
import org.jetbrains.bazel.ui.widgets.BazelJumpToBuildFileAction
import org.jetbrains.bazel.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id
import java.awt.Component
import java.awt.Point
import java.awt.event.MouseEvent

internal abstract class LoadedTargetsMouseListener(private val project: Project) : PopupHandler() {
  abstract fun isPointSelectable(point: Point): Boolean

  abstract fun getSelectedBuildTarget(): BuildTarget?

  abstract fun getSelectedDirectory(): VirtualFile?

  abstract val copyTargetIdAction: CopyTargetIdAction

  abstract val bazelJumpToBuildFileAction: BazelJumpToBuildFileAction

  abstract fun getSelectedComponentName(): String

  override fun mouseClicked(mouseEvent: MouseEvent) {
    if (mouseEvent.isDoubleClick() && isPointSelectable(mouseEvent.point)) {
      runOrBuildTarget(project, getSelectedBuildTarget() ?: return)
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
      ?: calculatePopupGroup(getSelectedDirectory() ?: return)

    ActionManager
      .getInstance()
      .createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, actionGroup)
      .component
      .show(component, x, y)
  }

  private fun calculatePopupGroup(target: BuildTarget): ActionGroup =
    DefaultActionGroup().apply {
      ResyncTargetAction.createIfEnabled(target.id)?.let { addAction(it) }
      addAction(copyTargetIdAction)
      addSeparator()
      addAction(BuildTargetAction(target.id))
      addAll(runReadActionBlocking { getExecutorActions(project, target) })  // We're on EDT so use runReadActionBlocking
      addAction(bazelJumpToBuildFileAction)
      add(StarlarkDebugAction(target.id))
    }

  private fun calculatePopupGroup(directory: VirtualFile): ActionGroup =
    DefaultActionGroup().apply {
      addAll(
        runReadActionBlocking {
          val psiDirectory = PsiManager.getInstance(project).findDirectory(directory)
          getExecutorActions(PsiLocation(psiDirectory))
        },
      )
    }

  private fun MouseEvent.isDoubleClick(): Boolean = this.mouseButton == MouseButton.Left && this.clickCount == 2
}

@ApiStatus.Internal
@RequiresEdt
fun runOrBuildTarget(project: Project, target: BuildTarget) {
  when (target.kind.ruleType) {
    RuleType.TEST, RuleType.BINARY -> {
      val location = BazelRunLocation(project, target)
      val dataContext = SimpleDataContext.builder()
        .add(CommonDataKeys.PROJECT, project)
        .add(Location.DATA_KEY, location)
        .build()
      val configurationContext = ConfigurationContext.getFromContext(dataContext, ActionPlaces.UNKNOWN)
      val settings = runReadActionBlocking {
        PreferredProducerFind.createConfiguration(location, configurationContext)
      } ?: return

      RunManagerEx.getInstanceEx(project).setTemporaryConfiguration(settings)
      val executor = DefaultRunExecutor.getRunExecutorInstance()
      val runner = ProgramRunner.getRunner(executor.id, settings.configuration) ?: return
      val executionEnvironment =
        ExecutionEnvironmentBuilder(project, executor)
          .runnerAndSettings(runner, settings)
          .build()
      runner.execute(executionEnvironment)
    }

    else -> {
      BuildTargetAction.buildTarget(project, target.id)
    }
  }
}
