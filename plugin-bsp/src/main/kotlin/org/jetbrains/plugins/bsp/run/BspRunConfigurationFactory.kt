package org.jetbrains.plugins.bsp.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

public class BspRunConfigurationFactory(configurationType: ConfigurationType) :
  ConfigurationFactory(configurationType) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return BspRunConfiguration(project, "BSP", this)
  }

  override fun getId(): String {
    return BspRunConfigurationType.ID
  }

  override fun getOptionsClass(): Class<out BaseState> {
    return BspRunConfigurationOptions::class.java
  }
}
