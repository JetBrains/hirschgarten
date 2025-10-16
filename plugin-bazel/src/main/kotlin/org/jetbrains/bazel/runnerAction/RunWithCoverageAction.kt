package org.jetbrains.bazel.runnerAction

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.bazelrunner.HasProgramArguments
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.test.setTestFilter
import org.jetbrains.bsp.protocol.BuildTarget

class RunWithCoverageAction(
  project: Project,
  targetInfos: List<BuildTarget>,
  text: ((isRunConfigName: Boolean) -> String)? = null,
  includeTargetNameInText: Boolean = false,
  private val singleTestFilter: String? = null,
  private val testExecutableArguments: List<String> = emptyList(),
) : BazelRunnerAction(
    targetInfos = targetInfos,
    text = { isRunConfigName ->
      if (text != null) {
        text(isRunConfigName || includeTargetNameInText)
      } else {
        BazelPluginBundle.message(
          "target.run.with.coverage.action.text",
          if (isRunConfigName ||
            includeTargetNameInText
          ) {
            targetInfos.joinToString(";") { it.id.toShortString(project) }
          } else {
            ""
          },
        )
      }
    },
    isDebugAction = false,
    isCoverageAction = true,
  ) {
  override fun RunnerAndConfigurationSettings.customizeRunConfiguration() {
    (configuration as BazelRunConfiguration).handler?.apply {
      setTestFilter(configuration.project, state, singleTestFilter)
    }
    (configuration as BazelRunConfiguration).handler?.apply {
      (state as? HasProgramArguments)?.programArguments?.addAll(testExecutableArguments)
    }
  }
}
