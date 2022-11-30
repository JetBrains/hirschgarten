package org.jetbrains.plugins.bsp.runConfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import org.jetbrains.plugins.bsp.config.BspPluginIcons

public class BspRunConfigurationType : ConfigurationTypeBase(
  ID,
  "BSP",
  "BSP run configuration",
  BspPluginIcons.bsp
) {

  init {
    addFactory(BspRunConfigurationFactory(this))
  }

  public val factory: ConfigurationFactory
    get() = configurationFactories.single()

  public companion object {
    public const val ID: String = "BspRunConfiguration"

    public fun getInstance(): BspRunConfigurationType =
      ConfigurationTypeUtil.findConfigurationType(BspRunConfigurationType::class.java)
  }
}
