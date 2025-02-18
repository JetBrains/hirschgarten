package org.jetbrains.bazel.run.config

import org.jetbrains.bazel.config.BuildToolId
import org.jetbrains.bazel.config.bspBuildToolId

class DefaultBspRunConfigurationTypeProvider : BspRunConfigurationTypeProvider {
  override val buildToolId: BuildToolId = bspBuildToolId
  override val runConfigurationType: BspRunConfigurationType = BspRunConfigurationType()
}
