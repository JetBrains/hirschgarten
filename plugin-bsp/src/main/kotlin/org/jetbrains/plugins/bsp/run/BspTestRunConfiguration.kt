package org.jetbrains.plugins.bsp.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.tasks.TestTargetTask
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.targetIdTOREMOVE

public class BspTestRunConfiguration(project: Project, configurationFactory: ConfigurationFactory, name: String) :
  RunConfigurationBase<String>(project, configurationFactory, name) {

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return RunProfileState { executor2, _ ->

      val bspTestConsole = BspConsoleService.getInstance(project).bspTestConsole

      val processHandler = BspProcessHandler()
      val testConsole = BspTestConsolePrinter(processHandler, SMTRunnerConsoleProperties(this, "BSP", executor2))
      environment.getUserData(targetIdTOREMOVE)?.let {
        bspTestConsole.registerPrinter(testConsole)
        processHandler.execute {
          try {
            // TODO error handling?
            TestTargetTask(project).executeIfConnected(it)
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
    TODO("Not yet implemented")
  }
}
