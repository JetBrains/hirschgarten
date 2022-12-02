package org.jetbrains.plugins.bsp.runConfig.test

import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.runConfig.BspRunConfiguration
import org.jetbrains.plugins.bsp.runConfig.BspRunStateBase

public class BspTestRunState(
  project: Project,
  environment: ExecutionEnvironment,
  configuration: BspRunConfiguration,
) : BspRunStateBase(project, environment, configuration) {

  init {
    consoleBuilder = BspTestConsoleBuilder(project, configuration, environment.executor)
  }

//    val target = options.target ?: "" // TODO: validate this earlier
//
//    bspRunConsole.registerPrinter(processHandler)
//    processHandler.execute {
//      val startRunMessage = "Running target $target"
//      processHandler.printOutput(startRunMessage)
//      try {
//        RunTargetTask(project).execute(BuildTargetIdentifier(target)).apply {
//          // TODO: execute's completable future may be cancelled or failed
//          when (statusCode) {
//            StatusCode.OK -> processHandler.printOutput("Successfully completed!")
//            StatusCode.CANCELLED -> processHandler.printOutput("Cancelled!")
//            StatusCode.ERROR -> processHandler.printOutput("Ended with an error!")
//            else -> processHandler.printOutput("Finished!")
//          }
//        }
//      } catch (e: Exception) {
//        processHandler.printOutput("Failed to run target $target")
//        processHandler.printOutput(e.message ?: "Unknown error")
//      } finally {
//        bspRunConsole.deregisterPrinter(processHandler)
//        processHandler.shutdown()
//      }
//    }
}
