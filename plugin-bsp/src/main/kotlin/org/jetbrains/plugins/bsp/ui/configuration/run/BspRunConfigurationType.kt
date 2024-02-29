package org.jetbrains.plugins.bsp.ui.configuration.run

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.ui.configuration.BspBaseRunConfiguration
import javax.swing.Icon

internal class BspRunConfigurationType(project: Project) : ConfigurationType {
  private val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)

  override fun getDisplayName(): String =
    BspPluginBundle.message("run.config.type.display.name", assetsExtension.presentableName)

  override fun getConfigurationTypeDescription(): String =
    BspPluginBundle.message("run.config.type.description", assetsExtension.presentableName)

  override fun getIcon(): Icon = assetsExtension.icon

  override fun getId(): String = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> =
    arrayOf(BspRunFactory(this))

  companion object {
    const val ID: String = "BspRunConfiguration"
  }
}

public class BspRunFactory(t: ConfigurationType) : ConfigurationFactory(t) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
    return BspRunConfiguration(project, this,
      BspPluginBundle.message("run.config.name", assetsExtension.presentableName))
  }

  override fun getId(): String =
    BspRunConfigurationType.ID
}

public class BspRunConfiguration(
  private val project: Project,
  configurationFactory: ConfigurationFactory,
  name: String,
) : BspBaseRunConfiguration(project, configurationFactory, name) {
  override var target: BuildTargetInfo? = null
    set(target) {
      field = target
      createRunHandlerFromTarget()
    }

  public var runHandler: BspRunHandler? = null

  private fun createRunHandlerFromTarget() {
    val target = target ?: return
    runHandler = BspRunHandler.getRunHandler(target).apply {
      prepareRunConfiguration(this@BspRunConfiguration)
    }
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    val target = target ?: return null
    return runHandler?.getRunProfileState(project, executor, environment, target)
  }

  override fun getBeforeRunTasks(): List<BeforeRunTask<*>> =
    runHandler?.getBeforeRunTasks(this) ?: emptyList()

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-627
    TODO("Not yet implemented")
  }
}
