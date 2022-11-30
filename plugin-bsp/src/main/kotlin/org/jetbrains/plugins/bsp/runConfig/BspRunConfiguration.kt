package org.jetbrains.plugins.bsp.runConfig

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.runConfig.run.BspRunState

public class BspRunConfiguration(
  project: Project,
  name: String,
  factory: ConfigurationFactory
) : LocatableConfigurationBase<BspRunConfigurationOptions>(project, factory, name) {
  override fun getOptions(): BspRunConfigurationOptions {
    return super.getOptions() as BspRunConfigurationOptions
  }
  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return BspRunConfigurationEditor(project)
  }

  override fun checkConfiguration() {
    // TODO: check if target exists and can be run with the selected run type
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    BspRunState(project, environment, this)
}
