package org.jetbrains.plugins.bsp.runnerAction

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import javax.swing.Icon

abstract class BspRunnerAction(
  targetInfo: BuildTargetInfo,
  text: () -> String,
  icon: Icon? = null,
  isDebugAction: Boolean = false,
) : BaseRunnerAction(targetInfo, text, icon, isDebugAction) {
  abstract fun getConfigurationType(project: Project): ConfigurationType

  open fun RunnerAndConfigurationSettings.customizeRunConfiguration() {}

  override suspend fun getRunnerSettings(project: Project, buildTargetInfo: BuildTargetInfo): RunnerAndConfigurationSettings? {
    val factory = getConfigurationType(project).configurationFactories.first()
    val name = calculateConfigurationName(project, buildTargetInfo)
    val settings =
      RunManager.getInstance(project).createConfiguration(name, factory)
    (settings.configuration as BspRunConfiguration)
      .updateTargets(listOf(buildTargetInfo.id))

    settings.customizeRunConfiguration()
    return settings
  }

  private fun calculateConfigurationName(project: Project, targetInfo: BuildTargetInfo): String {
    val targetDisplayName = targetInfo.buildTargetName
    val actionNameKey =
      when {
        this is TestTargetAction -> "target.test.config.name"
        else -> "target.run.config.name"
      }
    return BspPluginBundle.message(actionNameKey, targetDisplayName, project.assets.presentableName)
  }
}
