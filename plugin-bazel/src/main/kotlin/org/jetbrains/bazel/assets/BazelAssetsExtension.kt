package org.jetbrains.bazel.assets

import com.intellij.openapi.util.IconLoader
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.extension.points.BuildToolId
import javax.swing.Icon

internal class BazelAssetsExtension : BuildToolAssetsExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val presentableName: String = "Bazel"

  override val icon: Icon = BazelPluginIcons.bazel

  override val loadedTargetIcon: Icon = IconLoader.getIcon("/icons/bazel.svg", javaClass)
  override val unloadedTargetIcon: Icon = IconLoader.getIcon("/icons/grayBazel.svg", javaClass)
  override val invalidTargetIcon: Icon = IconLoader.getIcon("/icons/redBazel.svg", javaClass)
}
