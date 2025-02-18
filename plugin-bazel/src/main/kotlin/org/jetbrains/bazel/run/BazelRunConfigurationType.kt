package org.jetbrains.bazel.run

import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.config.BazelPluginConstants.BAZEL_DISPLAY_NAME
import org.jetbrains.bazel.run.config.BspRunConfigurationType

class BazelRunConfigurationType : BspRunConfigurationType(ID, BazelPluginIcons.bazel, BAZEL_DISPLAY_NAME) {
  companion object {
    const val ID: String = "BazelRunConfigurationType"
  }
}
