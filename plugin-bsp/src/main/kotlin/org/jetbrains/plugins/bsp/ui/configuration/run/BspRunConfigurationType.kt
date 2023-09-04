package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.server.tasks.RunTargetTask
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.configuration.test.BspConfigurationType
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.targetIdTOREMOVE
import javax.swing.Icon

internal class BspRunConfigurationType : ConfigurationType {
  override fun getDisplayName(): String = "BSP RUN"

  override fun getConfigurationTypeDescription(): String = "BSP RUN"

  override fun getIcon(): Icon = BspPluginIcons.bsp

  override fun getId(): String = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> =
    arrayOf(BspRunFactory(this))

  companion object {
    const val ID: String = "BSP_RUN_CONFIGURATION"
  }
}

public class BspRunFactory(t: ConfigurationType) : ConfigurationFactory(t) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration =
    BspRunConfiguration(project, this, "BSP RUN")

  override fun getId(): String =
    BspConfigurationType.ID
}

public class BspRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String) :
  RunConfigurationBase<String>(project, configurationFactory, name) {
  internal class BspCommandLineState(val project: Project, environment: ExecutionEnvironment) :
    CommandLineState(environment) {
    override fun startProcess(): BspProcessHandler = BspProcessHandler().apply {
      startNotify()
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
      val bspRunConsole = BspConsoleService.getInstance(project).bspRunConsole
      val processHandler = startProcess()
      val console = createConsole(executor)?.apply {
        attachToProcess(processHandler)
      }
      environment.getUserData(targetIdTOREMOVE)?.let { uri ->
        bspRunConsole.registerPrinter(processHandler)
        processHandler.execute {
          val startRunMessage = "Running target $uri"
          processHandler.printOutput(startRunMessage)
          try {
            RunTargetTask(project).connectAndExecute(BuildTargetIdentifier(uri))?.apply {
              when (statusCode) {
                StatusCode.OK -> processHandler.printOutput("Successfully completed!")
                StatusCode.CANCELLED -> processHandler.printOutput("Cancelled!")
                StatusCode.ERROR -> processHandler.printOutput("Ended with an error!")
                else -> processHandler.printOutput("Finished!")
              }
            }
          } finally {
            bspRunConsole.deregisterPrinter(processHandler)
            processHandler.shutdown()
          }
        }
      } ?: processHandler.shutdown()
      return DefaultExecutionResult(console, processHandler)
    }
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    BspCommandLineState(project, environment)

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-627
    TODO("Not yet implemented")
  }
}
