package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo

public abstract class BspBaseRunConfiguration(
  project: Project,
  configurationFactory: ConfigurationFactory,
  name: String,
) : RunConfigurationBase<String>(project, configurationFactory, name) {
  public open var target: BuildTargetInfo? = null
}
