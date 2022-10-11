package org.jetbrains.plugins.bsp.ui.configuration.test

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.project.stateStore
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.services.*
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import javax.swing.Icon


public class BspConfigurationType : ConfigurationType {

  override fun getDisplayName(): String = "BSP TEST"

  override fun getConfigurationTypeDescription(): String = "BSP TEST"

  override fun getIcon(): Icon = BspPluginIcons.bsp

  override fun getId(): String = ID

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(TestRunFactory(this))
  }
  public companion object {
    public const val ID: String = "BSP_TEST_RUN_CONFIGURATION"
  }
}

public class TestRunFactory(t: ConfigurationType) : ConfigurationFactory(t) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return TestRunConfiguration(project, this, "BSP TEST")
  }

  override fun getId(): String {
    return BspConfigurationType.ID
  }
}

public class TestRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String)
  : RunConfigurationBase<String>(project, configurationFactory, name) {

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return RunProfileState { executor2, _ ->

      val bspConnectionService = project.getService(BspConnectionService::class.java)
      val bspSyncConsoleService = BspSyncConsoleService.getInstance(project)
      val bspBuildConsoleService = BspBuildConsoleService.getInstance(project)
      val bspTestConsoleService = BspTestConsoleService.getInstance(project)

      val bspResolver = VeryTemporaryBspResolver(
        project.stateStore.projectBasePath,
        bspConnectionService.server!!,
        bspSyncConsoleService.bspSyncConsole,
        bspBuildConsoleService.bspBuildConsole
      )

      val processHandler = BspProcessHandler()
      val testConsole = BspTestConsole(processHandler, SMTRunnerConsoleProperties(this, "BSP", executor2))
      environment.getUserData(BspUtilService.targetIdKey)?.let {
        bspTestConsoleService.registerPrinter(testConsole)
        processHandler.execute {
          try {
            bspResolver.testTarget(it)
          } finally {
            testConsole.endTesting()
            bspTestConsoleService.deregisterPrinter(testConsole)
          }
        }
      } ?: processHandler.shutdown()
      DefaultExecutionResult(testConsole.console, processHandler)
    }
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    TODO("Not yet implemented")
  }

}