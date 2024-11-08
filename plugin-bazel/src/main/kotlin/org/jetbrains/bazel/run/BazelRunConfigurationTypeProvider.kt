package org.jetbrains.bazel.run

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.run.config.BspRunConfigurationTypeProvider

class BazelRunConfigurationTypeProvider : BspRunConfigurationTypeProvider {
  override val runConfigurationType: BazelRunConfigurationType = BazelRunConfigurationType()
  override val buildToolId: BuildToolId = bazelBspBuildToolId
}
