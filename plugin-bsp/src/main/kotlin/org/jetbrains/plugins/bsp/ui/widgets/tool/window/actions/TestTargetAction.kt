package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.ui.configuration.test.BspConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

internal class TestTargetAction : SideMenuRunnerAction(
  BspAllTargetsWidgetBundle.message("widget.test.target.popup.message")) {
  override fun getConfigurationType() = BspConfigurationType()

  override fun getName(target: BuildTargetId): String = target.substringAfter(':')
}
