package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.plugins.bsp.ui.configuration.test.BspConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

internal class TestTargetAction(
  target: BuildTargetIdentifier
) : SideMenuTargetAction(target, BspAllTargetsWidgetBundle.message("widget.test.target.popup.message")) {
  override fun getConfigurationType() = BspConfigurationType()

  override fun getName(target: BuildTargetIdentifier): String = target.uri.toString().substringAfter(':')
}
