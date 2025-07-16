package org.jetbrains.bazel.runnerAction

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.bazelrunner.HasProgramArguments
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bsp.protocol.BuildTarget

class TestTargetAction(
  project: Project,
  targetInfos: List<BuildTarget>,
  text: ((isRunConfigName: Boolean) -> String)? = null,
  isDebugAction: Boolean = false,
  includeTargetNameInText: Boolean = false,
  private val singleTestFilter: String? = null,
  private val testExecutableArguments: List<String> = emptyList(),
) : BazelRunnerAction(
    targetInfos = targetInfos,
    text = { isRunConfigName ->
      if (text != null) {
        text(isRunConfigName || includeTargetNameInText)
      } else if (isDebugAction && !isRunConfigName && !includeTargetNameInText) {
        BazelPluginBundle.message(
          "target.debug.test.action.text",
          "",
        )
      } else {
        BazelPluginBundle.message(
          "target.test.action.text",
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
    isDebugAction = isDebugAction,
  ) {
  override fun RunnerAndConfigurationSettings.customizeRunConfiguration() {
    (configuration as BazelRunConfiguration).handler?.apply { (state as? HasTestFilter)?.testFilter = singleTestFilter }
    (configuration as BazelRunConfiguration).handler?.apply {
      (state as? HasProgramArguments)?.programArguments?.addAll(testExecutableArguments)
    }
  }
}
