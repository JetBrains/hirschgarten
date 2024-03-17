package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.getBuildTargetName

internal class RunTargetAction(
  targetInfo: BuildTargetInfo,
  text: (() -> String)? = null,
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
}
