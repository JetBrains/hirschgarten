package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.configuration.RunConfigurationExtensionBase
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase

public typealias BspRunConfigurationExtension = RunConfigurationExtensionBase<BspRunConfigurationBase>

@Service
public class BspRunConfigurationExtensionManager :
  RunConfigurationExtensionsManager<BspRunConfigurationBase, BspRunConfigurationExtension>(ep) {
  public companion object {
    internal val ep =
      ExtensionPointName.create<BspRunConfigurationExtension>("org.jetbrains.bsp.bspRunConfigurationExtension")

    public fun getInstance(): BspRunConfigurationExtensionManager = service()
  }
}
