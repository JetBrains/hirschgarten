package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
abstract class BazelRunnerAction(
  private val project: Project,
  private val targets: List<ExecutableTarget>,
  executor: Executor,
  configurationName: String,
) : BaseRunnerAction(executor, configurationName) {

  private fun getConfigurationType(): ConfigurationType = runConfigurationType<BazelRunConfigurationType>()

  protected open fun RunnerAndConfigurationSettings.customizeRunConfiguration() {}

  override suspend fun getRunnerSettings(): RunnerAndConfigurationSettings? {
    return createRunConfiguration()
  }

  fun createRunConfiguration(): RunnerAndConfigurationSettings {
    val factory = getConfigurationType().configurationFactories.first()
    val settings =
      RunManager.getInstance(project).createConfiguration(configurationName, factory)
    (settings.configuration as BazelRunConfiguration)
      .updateTargets(targets.map { it.id }, RunHandlerProvider.getRunHandlerProvider(targets.map { it.kind }))

    settings.customizeRunConfiguration()
    return settings
  }
}
