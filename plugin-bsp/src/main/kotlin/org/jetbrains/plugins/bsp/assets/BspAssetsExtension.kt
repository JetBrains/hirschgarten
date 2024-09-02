package org.jetbrains.plugins.bsp.assets

import com.intellij.icons.AllIcons
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId
import javax.swing.Icon

public class BspAssetsExtension : BuildToolAssetsExtension {
  override val buildToolId: BuildToolId = bspBuildToolId

  override val presentableName: String = "BSP"

  override val targetIcon: Icon = BspPluginIcons.bsp
  override val errorTargetIcon: Icon = AllIcons.General.Error
  override val toolWindowIcon: Icon = BspPluginIcons.bsp
}
