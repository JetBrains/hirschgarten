package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id

@ApiStatus.Internal
class RunTargetAction(
  project: Project,
  target: BuildTarget,
  executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
  runnerActionDescriptor: BazelRunnerActionDescriptor? = null,
  callerPsiElement: PsiElement? = null,
) : BazelRunnerAction(
  project,
  targets = listOf(target),
  executor = executor,
  configurationName = target.id.toShortString(project),
  runnerActionDescriptor = runnerActionDescriptor,
  callerPsiElement = callerPsiElement,
)
