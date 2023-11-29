package org.jetbrains.plugins.bsp.assets

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import org.jetbrains.plugins.bsp.extension.points.bspBuildToolId
import javax.swing.Icon

public class BspAssetsExtension : BuildToolAssetsExtension {
  override val buildToolId: BuildToolId = bspBuildToolId

  override val presentableName: String = "BSP"

  override val icon: Icon = BspPluginIcons.bsp

  override val loadedTargetIcon: Icon = BspPluginIcons.bsp
  override val unloadedTargetIcon: Icon = IconLoader.getIcon("/icons/notLoaded.svg", javaClass)

  override val invalidTargetIcon: Icon = AllIcons.General.Warning
}
