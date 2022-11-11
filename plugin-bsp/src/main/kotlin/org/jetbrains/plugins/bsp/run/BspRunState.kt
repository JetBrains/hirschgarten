package org.jetbrains.plugins.bsp.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.tasks.RunTargetTask
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

public class BspRunState(
  private val project: Project,
  environment: ExecutionEnvironment,
  private val options: BspRunConfigurationOptions
) :
  CommandLineState(environment) {
  override fun startProcess(): BspProcessHandler = BspProcessHandler().apply {
    startNotify()
  }

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val bspRunConsole =
      BspConsoleService.getInstance(project).bspRunConsole
    val processHandler = startProcess()
    val console = createConsole(executor)?.apply {
      attachToProcess(processHandler)
    }
    val target = options.target ?: "" // TODO: validate this earlier

    bspRunConsole.registerPrinter(processHandler)
    processHandler.execute {
      val startRunMessage = "Running target $target"
      processHandler.printOutput(startRunMessage)
      try {
        RunTargetTask(project).execute(BuildTargetIdentifier(target)).apply {
          // TODO: execute's completable future may be cancelled or failed
          when (statusCode) {
            StatusCode.OK -> processHandler.printOutput("Successfully completed!")
            StatusCode.CANCELLED -> processHandler.printOutput("Cancelled!")
            StatusCode.ERROR -> processHandler.printOutput("Ended with an error!")
            else -> processHandler.printOutput("Finished!")
          }
        }
      } catch (e: Exception) {
        processHandler.printOutput("Failed to run target $target")
        processHandler.printOutput(e.message ?: "Unknown error")
      } finally {
        bspRunConsole.deregisterPrinter(processHandler)
        processHandler.shutdown()
      }
    }

    return DefaultExecutionResult(console, processHandler)
  }
}
