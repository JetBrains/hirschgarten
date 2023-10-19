package org.jetbrains.plugins.bsp.assets

import com.intellij.openapi.util.IconLoader
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.flow.open.BuildToolId
import javax.swing.Icon

public class BspAssetsExtension : BuildToolAssetsExtension {
  override val buildToolId: BuildToolId = BuildToolId("bsp")

  override val presentableName: String = "BSP"

  override val icon: Icon = BspPluginIcons.bsp

  override val loadedTargetIcon: Icon = BspPluginIcons.bsp
  override val unloadedTargetIcon: Icon = IconLoader.getIcon("/icons/notLoaded.svg", javaClass)
}

public class BazelAssetsExtension : BuildToolAssetsExtension {
  override val buildToolId: BuildToolId = BuildToolId("bazelbsp")

  override val presentableName: String = "Bazel"

  override val icon: Icon = IconLoader.getIcon("/icons/bazel.svg", javaClass)

  override val loadedTargetIcon: Icon = IconLoader.getIcon("/icons/bazel.svg", javaClass)
  override val unloadedTargetIcon: Icon = IconLoader.getIcon("/icons/grayBazel.svg", javaClass)
}
