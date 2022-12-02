package org.jetbrains.plugins.bsp.runConfig.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.runConfig.*
import org.jetbrains.plugins.bsp.server.tasks.RunTargetTask

public class BspRunState(
  project: Project,
  environment: ExecutionEnvironment,
  runConfiguration: BspRunConfiguration,
) :
  BspRunProfileStateBase(project, environment, runConfiguration, BspRunConsoleBuilder(project, runConfiguration)) {

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {

    val console = consoleBuilder.console
    val target = runConfiguration.state?.target ?: "" // TODO: validate this earlier
    val processHandler = BspProcessHandler(console)

    val startRunMessage = "Running target $target"
    console.print(startRunMessage, ConsoleViewContentType.SYSTEM_OUTPUT)
    try {
      RunTargetTask(project).execute(BuildTargetIdentifier(target)).apply {
        // TODO: execute's completable future may be cancelled or failed
        val endRunMessage = when (statusCode) {
          StatusCode.OK -> "Successfully completed!"
          StatusCode.CANCELLED -> "Cancelled!"
          StatusCode.ERROR -> "Ended with an error!"
          else -> "Finished!"
        }
        console.print(endRunMessage, ConsoleViewContentType.SYSTEM_OUTPUT)
      }
    } catch (e: Exception) {
      console.print("Failed to run target $target", ConsoleViewContentType.ERROR_OUTPUT)
      console.print(e.message ?: "Unknown error", ConsoleViewContentType.ERROR_OUTPUT)
    }
//
    return DefaultExecutionResult(console, processHandler)
  }
}
