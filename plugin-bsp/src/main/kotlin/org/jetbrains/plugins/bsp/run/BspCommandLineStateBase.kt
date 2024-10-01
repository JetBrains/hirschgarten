package org.jetbrains.plugins.bsp.run

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.server.connection.connection
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.taskEvents.BspTaskEventsService
import org.jetbrains.plugins.bsp.taskEvents.BspTaskListener
import org.jetbrains.plugins.bsp.taskEvents.OriginId

abstract class BspCommandLineStateBase(environment: ExecutionEnvironment, protected val originId: OriginId) :
  CommandLineState(environment) {
  protected abstract fun createAndAddTaskListener(handler: BspProcessHandler): BspTaskListener

  /** Run the actual BSP command or throw an exception if the server does not support running the configuration */
  protected abstract suspend fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities)

  final override fun startProcess(): BspProcessHandler {
    val configuration = environment.runProfile as BspRunConfiguration
    val project = configuration.project

    val bspCoroutineService = BspCoroutineService.getInstance(project)

    lateinit var handler: BspProcessHandler
    // We have to start runDeferred later, because we need to register the listener first
    // Otherwise, we might miss some events
    val runDeferred =
      bspCoroutineService.startAsync(lazy = true) {
        project.connection.runWithServer { server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities ->
          withContext(Dispatchers.EDT) {
            RunContentManager.getInstance(project).toFrontRunContent(environment.executor, handler)
          }
          startBsp(server, capabilities)
        }
      }

    handler = BspProcessHandler(runDeferred)
    val runListener = createAndAddTaskListener(handler)

    with(BspTaskEventsService.getInstance(project)) {
      saveListener(originId, runListener)
      runDeferred.invokeOnCompletion {
        removeListener(originId)
      }
    }

    runDeferred.start()

    return handler
  }
}
