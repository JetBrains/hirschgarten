package org.jetbrains.bazel.runnerAction

import com.intellij.execution.RunnerAndConfigurationSettings
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

class TestTargetAction(
  targetInfos: List<BuildTargetInfo>,
  text: ((includeTargetNameInText: Boolean) -> String)? = null,
  isDebugAction: Boolean = false,
  includeTargetNameInText: Boolean = false,
  private val singleTestFilter: String? = null,
) : BazelRunnerAction(
    targetInfos = targetInfos,
    text = { includeTargetNameInTextParam ->
      if (text != null) {
        text(includeTargetNameInTextParam || includeTargetNameInText)
      } else if (isDebugAction) {
        BazelPluginBundle.message(
          "target.debug.test.action.text",
          if (includeTargetNameInTextParam || includeTargetNameInText) targetInfos.joinToString(";") { it.buildTargetName } else "",
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      } else {
        BazelPluginBundle.message(
          "target.test.action.text",
          if (includeTargetNameInTextParam || includeTargetNameInText) targetInfos.joinToString(";") { it.buildTargetName } else "",
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      }
    },
    isDebugAction = isDebugAction,
  ) {
  override fun RunnerAndConfigurationSettings.customizeRunConfiguration() {
    (configuration as BazelRunConfiguration).handler?.apply { (state as? HasTestFilter)?.testFilter = singleTestFilter }
  }
}
