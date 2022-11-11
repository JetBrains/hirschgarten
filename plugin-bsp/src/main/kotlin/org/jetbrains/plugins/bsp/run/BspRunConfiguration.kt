package org.jetbrains.plugins.bsp.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

public class BspRunConfiguration(
  project: Project,
  name: String,
  factory: ConfigurationFactory
) : LocatableConfigurationBase<BspRunConfigurationOptions>(project, factory, name) {
  override fun getOptions(): BspRunConfigurationOptions {
    return super.getOptions() as BspRunConfigurationOptions
  }

  public var target: String?
    get() = options.target
    set(value) {
      options.target = value
    }

  public var runType: BspRunType
    get() = options.runType
    set(value) {
      options.runType = value
    }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return BspRunConfigurationEditor(project)
  }

  override fun checkConfiguration() {
    // TODO: check if target exists and can be run with the selected run type
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    BspRunState(project, environment, options)
}
