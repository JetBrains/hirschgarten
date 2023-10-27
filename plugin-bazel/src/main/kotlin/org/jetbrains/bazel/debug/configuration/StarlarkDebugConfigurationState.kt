package org.jetbrains.bazel.debug.configuration

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.debug.connector.StarlarkDebugManager
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler

class StarlarkDebugConfigurationState(
  val project: Project,
  environment: ExecutionEnvironment,
  port: Int,
) : CommandLineState(environment) {
  val manager = StarlarkDebugManager(project, port)

  override fun startProcess(): BspProcessHandler = BspProcessHandler().apply {
    startNotify()
  }

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val handler = startProcess()
    val console = createConsole(executor)?.apply {
      attachToProcess(handler)
    }
    return DefaultExecutionResult(console, handler)
  }
}
