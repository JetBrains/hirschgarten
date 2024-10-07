package org.jetbrains.plugins.bsp.run.config

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import javax.swing.Icon

class BspRunConfigurationType : SimpleConfigurationType {
  constructor() : this(BspPluginIcons.bsp)
  constructor(project: Project) : this(project.assets.toolWindowIcon)

  private constructor(icon: Icon) : super(
    id = ID,
    name = BspPluginBundle.message("runconfig.run.name"),
    description = BspPluginBundle.message("runconfig.run.description"),
    icon = NotNullLazyValue.createValue { icon },
  )

  override fun createTemplateConfiguration(project: Project): RunConfiguration = BspRunConfiguration(project, "")

  companion object {
    const val ID: String = "BspRunConfiguration"
  }
}
