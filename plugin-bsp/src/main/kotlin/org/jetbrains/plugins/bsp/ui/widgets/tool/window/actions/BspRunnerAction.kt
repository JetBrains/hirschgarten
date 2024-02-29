package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.configuration.BspBaseRunConfiguration
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.getBuildTargetName
import javax.swing.Icon

internal abstract class BspRunnerAction(
  targetInfo: BuildTargetInfo,
  text: () -> String,
  icon: Icon? = null,
  private val isDebugAction: Boolean = false,
) : BaseRunnerAction(targetInfo, text, icon, isDebugAction) {
  abstract fun getConfigurationType(project: Project): ConfigurationType

  override suspend fun getRunnerSettings(
    project: Project,
    buildTargetInfo: BuildTargetInfo,
  ): RunnerAndConfigurationSettings? {
    val factory = getConfigurationType(project).configurationFactories.first()
    val settings =
      RunManager.getInstance(project).createConfiguration(calculateConfigurationName(buildTargetInfo), factory)
    (settings.configuration as? BspBaseRunConfiguration)?.apply { target = buildTargetInfo }
    return settings
  }

  private fun calculateConfigurationName(targetInfo: BuildTargetInfo): String {
    val targetDisplayName = targetInfo.getBuildTargetName()
    val actionNameKey = when {
      isDebugAction -> "target.debug.config.name"
      this is TestTargetAction -> "target.test.config.name"
      else -> "target.run.config.name"
    }
    return BspPluginBundle.message(actionNameKey, targetDisplayName)
  }
}
