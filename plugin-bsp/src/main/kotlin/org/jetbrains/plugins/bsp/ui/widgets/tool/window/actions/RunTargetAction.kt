package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.configuration.run.BspDebugType
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationType
import javax.swing.Icon

internal class RunTargetAction(
  targetId: BuildTargetId,
  text: () -> String = { BspPluginBundle.message("widget.run.target.popup.message") },
  icon: Icon = AllIcons.Actions.Execute,
  private val debugType: BspDebugType? = null,
  private val useDebugMode: Boolean = false,
) : SideMenuRunnerAction(targetId, text, icon, useDebugExecutor = useDebugMode) {
  override fun getConfigurationType(project: Project): ConfigurationType = BspRunConfigurationType(project)

  override fun prepareRunConfiguration(configuration: RunConfiguration) {
    (configuration as? BspRunConfiguration)?.let {
      it.debugType = this.debugType
    }
  }

  override fun getName(target: BuildTargetId): String {
    val messageKey = if (useDebugMode) "console.task.debug.config.name" else "console.task.run.config.name"
    return BspPluginBundle.message(messageKey, target)
  }
}
