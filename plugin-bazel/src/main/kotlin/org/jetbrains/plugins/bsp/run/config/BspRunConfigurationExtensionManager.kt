package org.jetbrains.plugins.bsp.run.config

import com.intellij.execution.configuration.RunConfigurationExtensionBase
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName

public typealias BspRunConfigurationExtension = RunConfigurationExtensionBase<BspRunConfiguration>

@Service
public class BspRunConfigurationExtensionManager :
  RunConfigurationExtensionsManager<BspRunConfiguration, BspRunConfigurationExtension>(ep) {
  public companion object {
    internal val ep =
      ExtensionPointName.create<BspRunConfigurationExtension>("org.jetbrains.bsp.bspRunConfigurationExtension")

    public fun getInstance(): BspRunConfigurationExtensionManager = service()
  }
}
