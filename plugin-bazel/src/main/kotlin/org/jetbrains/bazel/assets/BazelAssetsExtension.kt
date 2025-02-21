package org.jetbrains.bazel.assets

import org.jetbrains.bazel.assets.BuildToolAssetsExtension
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.config.BuildToolId
import javax.swing.Icon

class BazelAssetsExtension : BuildToolAssetsExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override val presentableName: String = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override val targetIcon: Icon = BazelPluginIcons.bazel
  override val errorTargetIcon: Icon = BazelPluginIcons.bazelError
  override val toolWindowIcon: Icon = BazelPluginIcons.bazelToolWindow
}
