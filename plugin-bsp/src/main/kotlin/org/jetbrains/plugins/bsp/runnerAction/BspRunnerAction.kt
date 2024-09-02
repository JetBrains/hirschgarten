package org.jetbrains.plugins.bsp.runnerAction

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import javax.swing.Icon

abstract class BspRunnerAction(
  targetInfo: BuildTargetInfo,
  text: () -> String,
  icon: Icon? = null,
  private val isDebugAction: Boolean = false,
) : BaseRunnerAction(targetInfo, text, icon, isDebugAction) {
  abstract fun getConfigurationType(project: Project): ConfigurationType

  override suspend fun getRunnerSettings(project: Project, buildTargetInfo: BuildTargetInfo): RunnerAndConfigurationSettings? {
    val factory = getConfigurationType(project).configurationFactories.first()
    val name = calculateConfigurationName(buildTargetInfo)
    val settings =
      RunManager.getInstance(project).createConfiguration(name, factory)
    (settings.configuration as BspRunConfiguration)
      .updateTargets(listOf(buildTargetInfo.id))

    return settings
  }

  private fun calculateConfigurationName(targetInfo: BuildTargetInfo): String {
    val targetDisplayName = targetInfo.buildTargetName
    val actionNameKey =
      when {
        isDebugAction -> "target.debug.config.name"
        this is TestTargetAction -> "target.test.config.name"
        else -> "target.run.config.name"
      }
    return BspPluginBundle.message(actionNameKey, targetDisplayName)
  }
}
