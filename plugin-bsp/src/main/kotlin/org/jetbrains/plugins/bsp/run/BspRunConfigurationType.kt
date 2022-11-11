package org.jetbrains.plugins.bsp.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import javax.swing.Icon

public class BspRunConfigurationType : ConfigurationType {

  public companion object {
    public const val ID: String = "BspRunConfiguration"
  }

  override fun getDisplayName(): String {
    return "BSP"
  }

  override fun getConfigurationTypeDescription(): String {
    return "BSP run configuration"
  }

  override fun getIcon(): Icon {
    return BspPluginIcons.bsp
  }

  override fun getId(): String {
    return ID
  }

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(BspRunConfigurationFactory(this))
  }
}
