package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.icons.AllIcons
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.configuration.test.BspTestRunConfigurationType
import javax.swing.Icon

internal class TestTargetAction(
  targetId: BuildTargetId,
  text: () -> String = { BspPluginBundle.message("widget.test.target.popup.message") },
  icon: Icon = AllIcons.Actions.Execute,
) : SideMenuRunnerAction(targetId, text, icon) {
  override fun getConfigurationType() = BspTestRunConfigurationType()

  override fun getName(target: BuildTargetId): String = target.substringAfter(':')
}
