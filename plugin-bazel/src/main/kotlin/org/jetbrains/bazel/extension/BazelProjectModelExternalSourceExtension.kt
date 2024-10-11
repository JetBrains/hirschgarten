package org.jetbrains.bazel.extension

import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.extensionPoints.BspProjectModelExternalSourceExtension

class BazelProjectModelExternalSourceExtension : BspProjectModelExternalSourceExtension {
  override val buildToolId: BuildToolId = BazelPluginConstants.bazelBspBuildToolId

  override fun getDisplayName(): String = BazelPluginConstants.BAZEL_DISPLAY_NAME

  override fun getId(): String = BazelPluginConstants.ID
}
