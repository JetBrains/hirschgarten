package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.RunTargetTask
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.console.BspConsoleService

public open class GenericBspRunHandlerState(
  private val project: Project,
  environment: ExecutionEnvironment,
  private val targetId: String,
) : CommandLineState(environment) {
  protected open val debugType: BspDebugType? = null

  protected open val portForDebug: Int? = null

  override fun startProcess(): BspProcessHandler = BspProcessHandler().apply {
    startNotify()
  }

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val bspRunConsole = BspConsoleService.getInstance(project).bspRunConsole
    val processHandler = startProcess()
    val console = createConsole(executor)?.apply {
      attachToProcess(processHandler)
    }
    bspRunConsole.registerPrinter(processHandler)
    processHandler.execute {
      processHandler.printOutput(BspPluginBundle.message("console.task.run.start", targetId))
      try {
        val portForDebugIfApplicable = if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
          portForDebug
        } else {
          null
        }
        RunTargetTask(project, debugType, portForDebugIfApplicable)
          .connectAndExecute(BuildTargetIdentifier(targetId))
          ?.apply {
            when (statusCode) {
              StatusCode.OK -> processHandler.printOutput(BspPluginBundle.message("console.task.status.ok"))
              StatusCode.CANCELLED -> processHandler.printOutput(
                BspPluginBundle.message("console.task.status.cancelled")
              )

              StatusCode.ERROR -> processHandler.printOutput(BspPluginBundle.message("console.task.status.error"))
              else -> processHandler.printOutput(BspPluginBundle.message("console.task.status.other"))
            }
          }
      } finally {
        bspRunConsole.deregisterPrinter(processHandler)
        processHandler.shutdown()
      }
    }
    return DefaultExecutionResult(console, processHandler)
  }
}
