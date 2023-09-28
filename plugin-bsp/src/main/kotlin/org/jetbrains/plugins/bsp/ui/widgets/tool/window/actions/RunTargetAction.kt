package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.configurations.ConfigurationType
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationType
import javax.swing.Icon

internal class RunTargetAction(
  targetId: BuildTargetId,
  text: () -> String,
  icon: Icon,
) : SideMenuRunnerAction(targetId, text, icon) {
  override fun getConfigurationType(): ConfigurationType = BspRunConfigurationType()

  override fun getName(target: BuildTargetId): String = "run $target"
}
