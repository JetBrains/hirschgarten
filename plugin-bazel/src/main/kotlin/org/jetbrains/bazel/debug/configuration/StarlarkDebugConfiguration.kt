package org.jetbrains.bazel.debug.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Attribute
import org.jdom.Element

class StarlarkDebugConfiguration(
  project: Project,
  configurationFactory: ConfigurationFactory?,
  name: String,
) :
  RunConfigurationBase<StarlarkDebugConfiguration.Options>(project, configurationFactory, name),
  RunConfigurationWithSuppressedDefaultRunAction
{
  private val options = Options()

  override fun getOptions(): Options = options

  fun getPort(): Int = options.starlarkDebugPort

  fun setPort(port: Int) {
    options.starlarkDebugPort = port
  }

  override fun writeExternal(element: Element) {
    XmlSerializer.serializeInto(options, element)
  }

  override fun readExternal(element: Element) {
    XmlSerializer.deserializeInto(options, element)
  }

  override fun clone(): StarlarkDebugConfiguration =
    super.clone()
      .let { StarlarkDebugConfiguration(it.project, it.factory, it.name) }
      .also { it.options.starlarkDebugPort = this.options.starlarkDebugPort }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    StarlarkDebugConfigurationState(project, environment, getPort())

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = StarlarkDebugSettingsEditor()

  class Options : RunConfigurationOptions() {
    // platform code does not use bundle messages here
    // example: com.intellij.execution.application.JvmMainMethodRunConfigurationOptions
    @com.intellij.configurationStore.Property(description = "Port to use")
    @get:Attribute("starlark_debug_port")
    var starlarkDebugPort: Int by property(DEFAULT_PORT)
  }
}

private const val DEFAULT_PORT = 7300
