package org.jetbrains.bazel.runnerAction

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasTestFilter
import org.jetbrains.bsp.protocol.BuildTarget

class TestTargetAction(
  project: Project,
  targetInfos: List<BuildTarget>,
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
          if (includeTargetNameInTextParam ||
            includeTargetNameInText
          ) {
            targetInfos.joinToString(";") { it.id.toShortString(project) }
          } else {
            ""
          },
        )
      } else {
        BazelPluginBundle.message(
          "target.test.action.text",
          if (includeTargetNameInTextParam ||
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
  }
}
