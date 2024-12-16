package org.jetbrains.plugins.bsp.runnerAction

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.HasTestFilter
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

class TestTargetAction(
  targetInfos: List<BuildTargetInfo>,
  text: (() -> String)? = null,
  isDebugAction: Boolean = false,
  verboseText: Boolean = false,
  private val singleTestFilter: String? = null,
  project: Project,
) : BspRunnerAction(
    targetInfos = targetInfos,
    text = {
      if (text != null) {
        text()
      } else if (isDebugAction) {
        BspPluginBundle.message(
          "target.debug.test.action.text",
          if (verboseText) targetInfos.joinToString(";") { it.buildTargetName } else "",
          project.assets.presentableName,
        )
      } else {
        BspPluginBundle.message(
          "target.test.action.text",
          if (verboseText) targetInfos.joinToString(";") { it.buildTargetName } else "",
          project.assets.presentableName,
        )
      }
    },
    isDebugAction = isDebugAction,
  ) {
  override fun RunnerAndConfigurationSettings.customizeRunConfiguration() {
    (configuration as BspRunConfiguration).handler?.apply { (state as? HasTestFilter)?.testFilter = singleTestFilter }
  }
}
