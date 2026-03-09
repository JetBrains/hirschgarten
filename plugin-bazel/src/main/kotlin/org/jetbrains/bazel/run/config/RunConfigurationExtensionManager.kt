package org.jetbrains.bazel.run.config

import com.intellij.execution.configuration.RunConfigurationExtensionBase
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.extensions.ExtensionPointName

internal typealias RunConfigurationExtension = RunConfigurationExtensionBase<BazelRunConfiguration>

@Service
internal class RunConfigurationExtensionManager : RunConfigurationExtensionsManager<BazelRunConfiguration, RunConfigurationExtension>(ep) {
  public companion object {
    internal val ep =
      ExtensionPointName.create<RunConfigurationExtension>("org.jetbrains.bazel.runConfigurationExtension")

    public fun getInstance(): RunConfigurationExtensionManager = service()
  }
}
