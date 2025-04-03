package org.jetbrains.bazel.runnerAction

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bsp.protocol.BuildTarget

class RunWithCoverageAction(
  project: Project,
  targetInfos: List<BuildTarget>,
  text: ((includeTargetNameInText: Boolean) -> String)? = null,
  includeTargetNameInText: Boolean = false,
  private val singleTestFilter: String? = null,
) : BazelRunnerAction(
    targetInfos = targetInfos,
    text = { includeTargetNameInTextParam ->
      if (text != null) {
        text(includeTargetNameInTextParam || includeTargetNameInText)
      } else {
        BazelPluginBundle.message(
          "target.run.with.coverage.action.text",
          if (includeTargetNameInTextParam ||
            includeTargetNameInText
          ) {
            targetInfos.joinToString(";") { it.id.toShortString(project) }
          } else {
            ""
          },
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      }
    },
    isDebugAction = false,
    isCoverageAction = true,
  ) {
  override fun RunnerAndConfigurationSettings.customizeRunConfiguration() {
    (configuration as BazelRunConfiguration).handler?.apply { (state as? HasTestFilter)?.testFilter = singleTestFilter }
  }
}
