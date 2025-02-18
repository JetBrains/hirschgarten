package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.extensionPoints.BspToolWindowSettingsProviderExtension

class BazelProjectSettingsActionProviderExtension : BspToolWindowSettingsProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun getSettingsName(): String = BazelPluginBundle.message("project.settings.display.name")
}
