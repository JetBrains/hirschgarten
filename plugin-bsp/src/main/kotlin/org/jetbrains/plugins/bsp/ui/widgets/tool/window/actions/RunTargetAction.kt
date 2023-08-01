package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.configurations.ConfigurationType
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

internal class RunTargetAction : SideMenuRunnerAction(
        BspAllTargetsWidgetBundle.message("widget.run.target.popup.message")) {
  override fun getConfigurationType(): ConfigurationType = BspRunConfigurationType()

  override fun getName(target: BuildTargetId): String = "run $target"
}
