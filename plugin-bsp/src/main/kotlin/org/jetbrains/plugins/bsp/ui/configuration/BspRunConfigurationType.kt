package org.jetbrains.plugins.bsp.ui.configuration

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import javax.swing.Icon

public abstract class BspRunConfigurationTypeBase(
  id: String,
  name: String,
  description: String,
  icon: NotNullLazyValue<Icon>,
) : SimpleConfigurationType(id, name, description, icon)

public class BspRunConfigurationType : BspRunConfigurationTypeBase(
  id = ID,
  name = BspPluginBundle.message("runconfig.run.name"),
  description = BspPluginBundle.message("runconfig.run.description"),
  icon = NotNullLazyValue.createValue { BspPluginIcons.bsp },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration =
    BspRunConfiguration(project, this, name)

  public companion object {
    public const val ID: String = "BspRunConfiguration"
  }
}

public class BspTestConfigurationType : BspRunConfigurationTypeBase(
  id = ID,
  name = BspPluginBundle.message("runconfig.test.name"),
  description = BspPluginBundle.message("runconfig.test.description"),
  icon = NotNullLazyValue.createValue { BspPluginIcons.bsp },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration =
    BspTestConfiguration(project, this, name)

  public companion object {
    public const val ID: String = "BspTestConfiguration"
  }
}

public class BspBuildConfigurationType : BspRunConfigurationTypeBase(
  id = ID,
  name = BspPluginBundle.message("runconfig.build.name"),
  description = BspPluginBundle.message("runconfig.build.description"),
  icon = NotNullLazyValue.createValue { BspPluginIcons.bsp },
) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    TODO()
  }

  public companion object {
    public const val ID: String = "BspBuildConfiguration"
  }
}
