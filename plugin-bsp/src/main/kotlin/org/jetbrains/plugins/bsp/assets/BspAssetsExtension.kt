package org.jetbrains.plugins.bsp.assets

import com.intellij.icons.AllIcons
import javax.swing.Icon
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId

public class BspAssetsExtension : BuildToolAssetsExtension {
  override val buildToolId: BuildToolId = bspBuildToolId

  override val presentableName: String = "BSP"

  override val icon: Icon = BspPluginIcons.bsp

  override val targetIcon: Icon = BspPluginIcons.bsp

  override val invalidTargetIcon: Icon = AllIcons.General.Warning
}
