package org.jetbrains.bazel.debug.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import javax.swing.Icon

class StarlarkDebugConfigurationType : ConfigurationType {
  override fun getDisplayName(): String = StarlarkBundle.message("starlark.debug.config.type.name")

  override fun getConfigurationTypeDescription(): String =
    StarlarkBundle.message("starlark.debug.config.type.description")

  override fun getIcon(): Icon = BazelPluginIcons.bazel

  override fun getId(): String = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> =
    arrayOf(StarlarkDebugConfigurationFactory(this))

  companion object {
    const val ID = "RemoteStarlarkDebug"
  }
}
