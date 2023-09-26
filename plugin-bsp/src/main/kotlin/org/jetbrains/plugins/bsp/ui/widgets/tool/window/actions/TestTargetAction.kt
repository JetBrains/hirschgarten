package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.icons.AllIcons
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.configuration.test.BspConfigurationType

internal class TestTargetAction(targetId: BuildTargetId) : SideMenuRunnerAction(
  targetId = targetId,
  text = { BspPluginBundle.message("widget.test.target.popup.message") },
  icon = AllIcons.Toolwindows.ToolWindowRun,
) {
  override fun getConfigurationType() = BspConfigurationType()

  override fun getName(target: BuildTargetId): String = target.substringAfter(':')
}
