package org.jetbrains.bazel.runnerAction

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
import javax.swing.Icon

@ApiStatus.Internal
abstract class BazelRunnerAction(
  private val targetInfos: List<ExecutableTarget>,
  private val text: (isRunConfigName: Boolean) -> String,
  icon: Icon? = null,
  isDebugAction: Boolean = false,
  isCoverageAction: Boolean = false,
) : BaseRunnerAction({ text(false) }, icon, isDebugAction, isCoverageAction) {
  private fun getConfigurationType(): ConfigurationType = runConfigurationType<BazelRunConfigurationType>()

  protected open fun RunnerAndConfigurationSettings.customizeRunConfiguration() {}

  override fun getBuildTargets(project: Project): List<ExecutableTarget> = targetInfos

  override suspend fun getRunnerSettings(project: Project, targets: List<ExecutableTarget>): RunnerAndConfigurationSettings? {
    val factory = getConfigurationType().configurationFactories.first()
    val name = text(true)
    val settings =
      RunManager.getInstance(project).createConfiguration(name, factory)
    (settings.configuration as BazelRunConfiguration)
      .updateTargets(targets.map { it.id }, RunHandlerProvider.getRunHandlerProvider(targets.map { it.kind }))

    settings.customizeRunConfiguration()
    return settings
  }
}
