package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.execution.configurations.ConfigurationType
import org.jetbrains.plugins.bsp.run.BspRunConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

internal class RunTargetAction(
  target: BuildTargetIdentifier
) : SideMenuTargetAction(target, BspAllTargetsWidgetBundle.message("widget.run.target.popup.message")) {
  override fun getConfigurationType(): ConfigurationType = BspRunConfigurationType()

  override fun getName(target: BuildTargetIdentifier): String = "Run ${target.uri}"
}
