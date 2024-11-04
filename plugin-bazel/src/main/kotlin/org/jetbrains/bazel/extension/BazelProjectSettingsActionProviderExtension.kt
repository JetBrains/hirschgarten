package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.settings.BAZEL_PROJECT_SETTINGS_DISPLAY_NAME
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.extensionPoints.BspToolWindowSettingsProviderExtension

class BazelProjectSettingsActionProviderExtension : BspToolWindowSettingsProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun getSettingsName(): String = BAZEL_PROJECT_SETTINGS_DISPLAY_NAME
}
