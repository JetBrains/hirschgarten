package org.jetbrains.bazel.runnerAction

import com.intellij.execution.RunnerAndConfigurationSettings
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

class RunWithCoverageAction(
  targetInfos: List<BuildTargetInfo>,
  text: ((includeTargetNameInText: Boolean) -> String)? = null,
  includeTargetNameInText: Boolean = false,
  private val singleTestFilter: String? = null,
) : BspRunnerAction(
    targetInfos = targetInfos,
    text = { includeTargetNameInTextParam ->
      if (text != null) {
        text(includeTargetNameInTextParam || includeTargetNameInText)
      } else {
        BspPluginBundle.message(
          "target.run.with.coverage.action.text",
          if (includeTargetNameInTextParam || includeTargetNameInText) targetInfos.joinToString(";") { it.buildTargetName } else "",
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      }
    },
    isDebugAction = false,
    isCoverageAction = true,
  ) {
  override fun RunnerAndConfigurationSettings.customizeRunConfiguration() {
    (configuration as BspRunConfiguration).handler?.apply { (state as? HasTestFilter)?.testFilter = singleTestFilter }
  }
}
