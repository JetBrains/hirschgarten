package org.jetbrains.bazel.assets

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BuildToolId
import javax.swing.Icon

class BazelAssetsExtension : BuildToolAssetsExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val presentableName: String = "Bazel"

  override val targetIcon: Icon = BazelPluginIcons.bazel
  override val errorTargetIcon: Icon = BazelPluginIcons.bazelError
  override val toolWindowIcon: Icon = BazelPluginIcons.bazelToolWindow
}
