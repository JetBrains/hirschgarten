package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.extensionPoints.BspToolWindowSettingsProviderExtension

class BazelProjectSettingsActionProviderExtension : BspToolWindowSettingsProviderExtension {
  override val buildToolId: BuildToolId = bazelBspBuildToolId

  override fun getSettingsName(): String = BazelPluginBundle.message("project.settings.display.name")
}
