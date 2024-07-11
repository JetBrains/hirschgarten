package org.jetbrains.plugins.bsp.ui.actions.target

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.BspTestConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.getBuildTargetName

public class TestTargetAction(
  targetInfo: BuildTargetInfo,
  text: (() -> String)? = null,
  isDebugAction: Boolean = false,
  verboseText: Boolean = false,
) : BspRunnerAction(
  targetInfo = targetInfo,
  text = {
    if (text != null) text()
    else if (isDebugAction) BspPluginBundle.message(
      "target.debug.action.text",
      if (verboseText) targetInfo.getBuildTargetName() else ""
    )
    else BspPluginBundle.message(
      "target.test.action.text",
      if (verboseText) targetInfo.getBuildTargetName() else ""
    )
  },
  isDebugAction = isDebugAction) {
  override fun getConfigurationType(project: Project): ConfigurationType = BspTestConfigurationType()
}
