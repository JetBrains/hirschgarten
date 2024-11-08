package org.jetbrains.plugins.bsp.run.config

import org.jetbrains.plugins.bsp.config.BuildToolId
import org.jetbrains.plugins.bsp.config.bspBuildToolId

class DefaultBspRunConfigurationTypeProvider : BspRunConfigurationTypeProvider {
  override val buildToolId: BuildToolId = bspBuildToolId
  override val runConfigurationType: BspRunConfigurationType = BspRunConfigurationType()
}
