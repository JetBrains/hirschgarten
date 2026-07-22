package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
class RunTargetAction(
  project: Project,
  target: ExecutableTarget,
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
