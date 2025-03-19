package org.jetbrains.bazel.runnerAction

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import javax.swing.Icon

abstract class BazelRunnerAction(
  targetInfos: List<BuildTargetInfo>,
  private val text: (includeTargetNameInText: Boolean) -> String,
  icon: Icon? = null,
  isDebugAction: Boolean = false,
  isCoverageAction: Boolean = false,
) : BaseRunnerAction(targetInfos, { text(false) }, icon, isDebugAction, isCoverageAction) {
  fun getConfigurationType(project: Project): ConfigurationType = BazelRunConfigurationType()

  open fun RunnerAndConfigurationSettings.customizeRunConfiguration() {}

  override suspend fun getRunnerSettings(project: Project, buildTargetInfos: List<BuildTargetInfo>): RunnerAndConfigurationSettings? {
    val factory = getConfigurationType(project).configurationFactories.first()
    val name = text(true)
    val settings =
      RunManager.getInstance(project).createConfiguration(name, factory)
    (settings.configuration as BazelRunConfiguration)
      .updateTargets(buildTargetInfos.map { it.id })

    settings.customizeRunConfiguration()
    return settings
  }
}
