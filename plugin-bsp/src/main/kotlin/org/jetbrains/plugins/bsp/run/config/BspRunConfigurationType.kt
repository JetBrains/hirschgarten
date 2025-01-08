package org.jetbrains.plugins.bsp.run.config

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.bsp.protocol.BSP_DISPLAY_NAME
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import javax.swing.Icon

// We must access the icon directly, not through the assets extension, because the tool id may not be set yet.
open class BspRunConfigurationType : SimpleConfigurationType {
  constructor() : this(ID, BspPluginIcons.bsp, BSP_DISPLAY_NAME)

  constructor(id: String, icon: Icon, buildToolName: String) : super(
    id = id,
    name = buildToolName,
    description = BspPluginBundle.message("runconfig.run.description", buildToolName),
    icon = NotNullLazyValue.createValue { icon },
  )

  override fun createTemplateConfiguration(project: Project): RunConfiguration = BspRunConfiguration(project, "", this)

  companion object {
    const val ID: String = "BspRunConfigurationType"
  }
}
