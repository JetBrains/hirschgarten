package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.extensionPoints.BspToolWindowSettingsProviderExtension
import org.jetbrains.bazel.ui.settings.BAZEL_PROJECT_SETTINGS_DISPLAY_NAME

class BazelProjectSettingsActionProviderExtension : BspToolWindowSettingsProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun getSettingsName(): String = BAZEL_PROJECT_SETTINGS_DISPLAY_NAME
}
