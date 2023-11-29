package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.assets.BuildToolAssetsExtension
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.buildToolId
import org.jetbrains.plugins.bsp.extension.points.withBuildToolIdOrDefault
import org.jetbrains.plugins.bsp.server.tasks.RunTargetTask
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
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
  project: Project,
  configurationFactory: ConfigurationFactory,
  name: String,
) : RunConfigurationBase<String>(project, configurationFactory, name) {
  public var debugType: BspDebugType? = null
  public var targetUri: String? = null

  internal class BspCommandLineState(
    val project: Project,
    environment: ExecutionEnvironment,
    private val targetUri: String?,
    private val debugType: BspDebugType?,
  ) : CommandLineState(environment) {
    val remoteConnection: RemoteConnection? = createRemoteConnection()

    private fun createRemoteConnection(): RemoteConnection? =
      when (debugType) {
        BspDebugType.JDWP -> RemoteConnection(true, "localhost", "0", true)
        else -> null
      }

    override fun startProcess(): BspProcessHandler = BspProcessHandler().apply {
      startNotify()
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
      val bspRunConsole = BspConsoleService.getInstance(project).bspRunConsole
      val processHandler = startProcess()
      val console = createConsole(executor)?.apply {
        attachToProcess(processHandler)
      }
      targetUri?.let { uri ->
        bspRunConsole.registerPrinter(processHandler)
        processHandler.execute {
          processHandler.printOutput(BspPluginBundle.message("console.task.run.start", uri))
          try {
            val portForDebugIfApplicable = if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
              remoteConnection?.debuggerAddress?.toInt()
            } else null
            RunTargetTask(project, debugType, portForDebugIfApplicable)
              .connectAndExecute(BuildTargetIdentifier(uri))
              ?.apply {
                when (statusCode) {
                  StatusCode.OK -> processHandler.printOutput(BspPluginBundle.message("console.task.status.ok"))
                  StatusCode.CANCELLED -> processHandler.printOutput(
                    BspPluginBundle.message("console.task.status.cancelled"))
                  StatusCode.ERROR -> processHandler.printOutput(BspPluginBundle.message("console.task.status.error"))
                  else -> processHandler.printOutput(BspPluginBundle.message("console.task.status.other"))
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
    BspCommandLineState(project, environment, targetUri, debugType)

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    // TODO https://youtrack.jetbrains.com/issue/BAZEL-627
    TODO("Not yet implemented")
  }
}
