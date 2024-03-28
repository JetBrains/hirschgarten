package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationEditor
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunHandler

public abstract class BspRunConfigurationBase(
  project: Project,
  configurationFactory: BspRunConfigurationTypeBase,
  name: String,
) : LocatableConfigurationBase<RunProfileState>(project, configurationFactory, name),
  RunConfigurationWithSuppressedDefaultDebugAction,
  DumbAware {
  public var targets: List<BuildTargetInfo> = emptyList()
    set(value) {
      runHandler = BspRunHandler.getRunHandler(value)
      runHandler.prepareRunConfiguration(this)
      field = value
    }

  public var env: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT
  public var runHandler: BspRunHandler = BspRunHandler.getRunHandler(targets)

  override fun getConfigurationEditor(): SettingsEditor<out BspRunConfigurationBase> =
    BspRunConfigurationEditor(this)

  override fun getBeforeRunTasks(): List<BeforeRunTask<*>> =
    runHandler.getBeforeRunTasks(this)
}

public class BspRunConfiguration(
  project: Project,
  configurationFactory: BspRunConfigurationType,
  name: String,
) : BspRunConfigurationBase(project, configurationFactory, name) {
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    runHandler.getRunProfileState(project, executor, environment, this)

  override fun checkConfiguration() {
    // TODO: check if targetUri is valid
    // Check if target can be debugged
    // Check if target can be run/tested
  }
}

public class BspTestConfiguration(
  project: Project,
  configurationFactory: BspTestConfigurationType,
  name: String,
) : BspRunConfigurationBase(project, configurationFactory, name),
  SMRunnerConsolePropertiesProvider {
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    runHandler.getRunProfileState(project, executor, environment, this)

  override fun checkConfiguration() {
    // TODO: check if targetUri is valid
    // Check if target can be debugged
    // Check if target can be run/tested
  }

  override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties =
    SMTRunnerConsoleProperties(this, "BSP", executor)
}

public class BspBuildConfiguration(
  project: Project,
  configurationFactory: BspBuildConfigurationType,
  name: String,
) : BspRunConfigurationBase(project, configurationFactory, name) {
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    runHandler.getRunProfileState(project, executor, environment, this)
}
