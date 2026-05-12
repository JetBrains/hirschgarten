package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasProgramArguments
import org.jetbrains.bazel.run.test.setTestFilter
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
class TestTargetAction(
  project: Project,
  targets: List<ExecutableTarget>,
  executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
  configurationName: String,
  private val singleTestFilter: String? = null,
  private val testExecutableArguments: List<String> = emptyList(),
  callerPsiElement: PsiElement? = null,
) : BazelRunnerAction(
  project = project,
  targets = targets,
  executor = executor,
  configurationName = configurationName,
  callerPsiElement = callerPsiElement,
) {
  constructor(
    project: Project,
    target: ExecutableTarget,
    executor: Executor = DefaultRunExecutor.getRunExecutorInstance(),
    singleTestFilter: String? = null,
    testExecutableArguments: List<String> = emptyList(),
    callerPsiElement: PsiElement? = null,
  ) : this(
    project = project,
    targets = listOf(target),
    executor = executor,
    configurationName = target.id.toShortString(project),
    singleTestFilter = singleTestFilter,
    testExecutableArguments = testExecutableArguments,
    callerPsiElement = callerPsiElement,
  )

  override fun RunnerAndConfigurationSettings.customizeRunConfiguration() {
    (configuration as BazelRunConfiguration).handler?.apply {
      setTestFilter(configuration.project, state, singleTestFilter)
    }
    (configuration as BazelRunConfiguration).handler?.apply {
      (state as? HasProgramArguments)?.programArguments = formatProgramArguments(testExecutableArguments)
    }
  }

  private fun formatProgramArguments(arguments: List<String>): String =
    arguments.joinToString(" ") { argument ->
      val escaped = argument.replace("\"", "\\\"")
      "\"$escaped\""
    }
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
