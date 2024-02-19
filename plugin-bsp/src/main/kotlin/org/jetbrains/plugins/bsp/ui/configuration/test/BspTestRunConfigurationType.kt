package org.jetbrains.plugins.bsp.ui.configuration.test

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.server.tasks.TestTargetTask
import org.jetbrains.plugins.bsp.ui.configuration.BspBaseRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import javax.swing.Icon

internal class BspTestRunConfigurationType(project: Project) : ConfigurationType {
  private val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)

  override fun getDisplayName(): String =
    BspPluginBundle.message("test.config.type.display.name", assetsExtension.presentableName)

  override fun getConfigurationTypeDescription(): String =
    BspPluginBundle.message("test.config.type.description", assetsExtension.presentableName)

  override fun getIcon(): Icon = assetsExtension.icon

  override fun getId(): String = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> =
    arrayOf(BspTestRunFactory(this))

  companion object {
    const val ID: String = "BspTestRunConfiguration"
  }
}

internal class BspTestRunFactory(t: ConfigurationType) : ConfigurationFactory(t) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    val assetsExtension = BuildToolAssetsExtension.ep.withBuildToolIdOrDefault(project.buildToolId)
    return BspTestRunConfiguration(project, this, BspPluginBundle.message("test.config.name", assetsExtension.presentableName))
  }

  override fun getId(): String =
    BspTestRunConfigurationType.ID
}

internal class BspTestRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String) :
  BspBaseRunConfiguration(project, configurationFactory, name) {
  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return RunProfileState { executor2, _ ->

      val bspTestConsole = BspConsoleService.getInstance(project).bspTestConsole

      val processHandler = BspProcessHandler()
      val testConsole = BspTestConsolePrinter(processHandler, SMTRunnerConsoleProperties(this, "BSP", executor2))
      targetUri?.let { uri ->
        bspTestConsole.registerPrinter(testConsole)
        processHandler.execute {
          try {
            // TODO error handling?
            TestTargetTask(project).connectAndExecute(BuildTargetIdentifier(uri))
          } finally {
            testConsole.endTesting()
            bspTestConsole.deregisterPrinter(testConsole)
          }
        }
      } ?: processHandler.shutdown()
      DefaultExecutionResult(testConsole.console, processHandler)
    }
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-628
    TODO("Not yet implemented")
  }
}
