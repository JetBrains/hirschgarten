package org.jetbrains.bazel.ui.widgets

import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.runnerAction.RunWithLocalJvmRunnerAction
import org.jetbrains.bazel.runnerAction.TestWithLocalJvmRunnerAction
import org.jetbrains.bazel.settings.bazel.bazelJVMProjectSettings
import org.jetbrains.bazel.ui.widgets.tool.window.utils.LoadedTargetActionsProvider
import org.jetbrains.bsp.protocol.BuildTarget

internal class JvmLoadedTargetActionsProvider : LoadedTargetActionsProvider {
  override fun onActionsInit(
    project: Project,
    group: DefaultActionGroup,
    target: BuildTarget,
    includeTargetNameInText: Boolean,
    callerPsiElement: PsiElement?,
  ) {
    val kind = target.kind
    if (project.bazelJVMProjectSettings.enableLocalJvmActions && kind.isJvmTarget()) {
      if (kind.ruleType == RuleType.BINARY) {
        group.addAction(RunWithLocalJvmRunnerAction(project, target, includeTargetNameInText = includeTargetNameInText))
        group.addAction(RunWithLocalJvmRunnerAction(project, target, isDebugMode = true, includeTargetNameInText = includeTargetNameInText))
      }
      if (kind.ruleType == RuleType.TEST) {
        if (callerPsiElement != null) { // called from gutter
          group.addLocalJvmTestActions(project, target, includeTargetNameInText, callerPsiElement)
        }
        else if (!project.bazelJVMProjectSettings.useIntellijTestRunner) { // called from target tree widget
          group.addLocalJvmTestActions(project, target, includeTargetNameInText, null)
        }
      }
    }
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
}
