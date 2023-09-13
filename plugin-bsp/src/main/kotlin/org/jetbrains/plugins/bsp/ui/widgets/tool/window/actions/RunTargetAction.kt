package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.configurations.ConfigurationType
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationType

internal class RunTargetAction : SideMenuRunnerAction(
  BspPluginBundle.message("widget.run.target.popup.message"),
) {
  override fun getConfigurationType(): ConfigurationType = BspRunConfigurationType()

  override fun getName(target: BuildTargetId): String = "run $target"
}
