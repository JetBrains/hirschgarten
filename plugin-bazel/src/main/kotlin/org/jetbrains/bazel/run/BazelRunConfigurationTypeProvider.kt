package org.jetbrains.bazel.run

import org.jetbrains.bazel.config.BazelPluginConstants.bazelBspBuildToolId
import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.run.config.BspRunConfigurationTypeProvider

class BazelRunConfigurationTypeProvider : BspRunConfigurationTypeProvider {
  override val runConfigurationType: BazelRunConfigurationType = BazelRunConfigurationType()
  override val buildToolId: BuildToolId = bazelBspBuildToolId
}
