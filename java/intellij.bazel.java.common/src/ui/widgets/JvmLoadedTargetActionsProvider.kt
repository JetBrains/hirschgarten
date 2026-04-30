package org.jetbrains.bazel.ui.widgets

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.runnerAction.RunWithLocalJvmRunnerAction
import org.jetbrains.bazel.runnerAction.TestWithLocalJvmRunnerAction
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.ui.widgets.tool.window.utils.LoadedTargetActionsProvider
import org.jetbrains.bsp.protocol.ExecutableTarget

internal class JvmLoadedTargetActionsProvider : LoadedTargetActionsProvider {
  override fun onActionsInit(
    project: Project,
    group: DefaultActionGroup,
    target: ExecutableTarget,
    callerPsiElement: PsiElement?,
  ) {
    val kind = target.kind
    if (project.bazelJVMProjectSettings.enableLocalJvmActions && kind.isJvmTarget()) {
      if (kind.ruleType == RuleType.BINARY) {
        group.addAction(RunWithLocalJvmRunnerAction(project, target))
        group.addAction(RunWithLocalJvmRunnerAction(project, target, executor = DefaultDebugExecutor.getDebugExecutorInstance()))
      }
      if (kind.ruleType == RuleType.TEST) {
        if (callerPsiElement != null) { // called from gutter
          group.addLocalJvmTestActions(project, target, callerPsiElement)
        }
        else if (!project.bazelJVMProjectSettings.useIntellijTestRunner) { // called from target tree widget
          group.addLocalJvmTestActions(project, target, null)
        }
      }
    }
  }

  private fun DefaultActionGroup.addLocalJvmTestActions(
    project: Project,
    target: ExecutableTarget,
    callerPsiElement: PsiElement?,
  ) {
    addAction(
      TestWithLocalJvmRunnerAction(project, target, callerPsiElement = callerPsiElement),
    )
    addAction(
      TestWithLocalJvmRunnerAction(
        project,
        target,
        executor = DefaultDebugExecutor.getDebugExecutorInstance(),
        callerPsiElement = callerPsiElement,
      ),
    )
  }
}
