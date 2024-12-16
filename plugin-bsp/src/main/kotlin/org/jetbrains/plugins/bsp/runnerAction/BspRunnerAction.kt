package org.jetbrains.plugins.bsp.runnerAction

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.config.BspRunConfigurationTypeProvider
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import javax.swing.Icon

abstract class BspRunnerAction(
  targetInfos: List<BuildTargetInfo>,
  text: () -> String,
  icon: Icon? = null,
  isDebugAction: Boolean = false,
) : BaseRunnerAction(targetInfos, text, icon, isDebugAction) {
  fun getConfigurationType(project: Project): ConfigurationType = BspRunConfigurationTypeProvider.getConfigurationType(project)

  open fun RunnerAndConfigurationSettings.customizeRunConfiguration() {}

  override suspend fun getRunnerSettings(project: Project, buildTargetInfos: List<BuildTargetInfo>): RunnerAndConfigurationSettings? {
    val factory = getConfigurationType(project).configurationFactories.first()
    val name = calculateConfigurationName(project, buildTargetInfos)
    val settings =
      RunManager.getInstance(project).createConfiguration(name, factory)
    (settings.configuration as BspRunConfiguration)
      .updateTargets(buildTargetInfos.map { it.id })

    settings.customizeRunConfiguration()
    return settings
  }

  private fun calculateConfigurationName(project: Project, targetInfo: List<BuildTargetInfo>): String {
    val targetDisplayName = targetInfo.map { it.id }.joinToString(";")
    val actionNameKey =
      when {
        this is TestTargetAction -> "target.test.config.name"
        else -> "target.run.config.name"
      }
    return BspPluginBundle.message(actionNameKey, targetDisplayName, project.assets.presentableName)
  }
}
