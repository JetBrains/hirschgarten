package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
class TestTargetAction(
  project: Project,
  targets: List<ExecutableTarget>,
  executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
  configurationName: String,
  runnerActionDescriptor: BazelRunnerActionDescriptor? = null,
  private val testExecutableArguments: List<String> = emptyList(),
  callerPsiElement: PsiElement? = null,
) : BazelRunnerAction(
  project = project,
  targets = targets,
  executor = executor,
  configurationName = configurationName,
  runnerActionDescriptor = runnerActionDescriptor,
  callerPsiElement = callerPsiElement,
) {
  constructor(
    project: Project,
    target: ExecutableTarget,
    executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
    runnerActionDescriptor: BazelRunnerActionDescriptor? = null,
    callerPsiElement: PsiElement? = null,
  ) : this(
    project = project,
    targets = listOf(target),
    executor = executor,
    configurationName = target.id.toShortString(project),
    runnerActionDescriptor = runnerActionDescriptor,
    callerPsiElement = callerPsiElement,
  )
}

@ApiStatus.Internal
fun getTestExecutors(): List<Executor> = listOfNotNull(
  DefaultRunExecutor.getRunExecutorInstance(),
  getCoverageExecutor(),
)

@ApiStatus.Internal
const val COVERAGE_EXECUTOR_ID: String = "Coverage"

@ApiStatus.Internal
fun getCoverageExecutor(): Executor? = Executor.EXECUTOR_EXTENSION_NAME.findFirstSafe { it.id == COVERAGE_EXECUTOR_ID }
