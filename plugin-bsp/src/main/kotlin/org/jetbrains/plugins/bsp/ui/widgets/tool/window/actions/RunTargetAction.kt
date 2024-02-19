package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.configuration.run.BspDebugType
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.getBuildTargetName

internal class RunTargetAction(
  targetInfo: BuildTargetInfo,
  text: (() -> String)? = null,
  private val debugType: BspDebugType? = null,
  isDebugAction: Boolean = false,
  verboseText: Boolean = false,
) : BspRunnerAction(
  targetInfo = targetInfo,
  text = {
    if (text != null) text()
    else if (isDebugAction) BspPluginBundle.messageVerbose(
      "target.debug.action.text",
      verboseText,
      targetInfo.getBuildTargetName()
    )
    else BspPluginBundle.messageVerbose(
      "target.run.action.text",
      verboseText,
      targetInfo.getBuildTargetName()
    )
  },
  isDebugAction = isDebugAction,
) {
  override fun getConfigurationType(project: Project): ConfigurationType = BspRunConfigurationType(project)

  override fun prepareRunConfiguration(runConfiguration: RunConfiguration) {
    (runConfiguration as? BspRunConfiguration)?.let {
      it.debugType = this.debugType
    }
  }
}
