package org.jetbrains.bazel.debug.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class StarlarkDebugConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration =
    StarlarkDebugConfiguration(project, this, "")

  override fun getOptionsClass(): Class<out BaseState> =
    StarlarkDebugConfiguration.Options::class.java

  override fun getId(): String = "StarlarkDebugFactory"
}
