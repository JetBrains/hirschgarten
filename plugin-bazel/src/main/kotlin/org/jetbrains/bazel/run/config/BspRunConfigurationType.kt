package org.jetbrains.bazel.run.config

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.config.BspPluginIcons
import org.jetbrains.bsp.protocol.BSP_DISPLAY_NAME
import javax.swing.Icon

// We must access the icon directly, not through the assets extension, because the tool id may not be set yet.
open class BspRunConfigurationType :
  SimpleConfigurationType,
  DumbAware {
  constructor() : this(ID, BspPluginIcons.bsp, BSP_DISPLAY_NAME)

  constructor(id: String, icon: Icon, buildToolName: String) : super(
    id = id,
    name = buildToolName,
    description = BspPluginBundle.message("runconfig.run.description", buildToolName),
    icon = NotNullLazyValue.createValue { icon },
  )

  override fun createTemplateConfiguration(project: Project): RunConfiguration = BspRunConfiguration(project, "", this)

  override fun isEditableInDumbMode(): Boolean = true

  companion object {
    const val ID: String = "BspRunConfigurationType"
  }
}
