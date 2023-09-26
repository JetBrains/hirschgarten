package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationType

internal class RunTargetAction(targetId: BuildTargetId) : SideMenuRunnerAction(
  targetId = targetId,
  text = { BspPluginBundle.message("widget.run.target.popup.message") },
  icon = AllIcons.Toolwindows.ToolWindowRun
) {
  override fun getConfigurationType(): ConfigurationType = BspRunConfigurationType()

  override fun getName(target: BuildTargetId): String = "run $target"
}
