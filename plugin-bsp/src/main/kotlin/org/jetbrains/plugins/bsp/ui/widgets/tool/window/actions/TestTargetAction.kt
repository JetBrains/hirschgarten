package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.ui.configuration.test.BspConfigurationType
import javax.swing.Icon

internal class TestTargetAction(
  targetId: BuildTargetId,
  text: () -> String,
  icon: Icon,
) : SideMenuRunnerAction(targetId, text, icon) {
  override fun getConfigurationType() = BspConfigurationType()

  override fun getName(target: BuildTargetId): String = target.substringAfter(':')
}
