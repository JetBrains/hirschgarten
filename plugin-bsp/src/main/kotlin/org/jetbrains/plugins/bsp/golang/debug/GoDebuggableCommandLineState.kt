package org.jetbrains.plugins.bsp.golang.debug


import com.goide.execution.application.GoApplicationConfiguration
import com.goide.execution.application.GoApplicationRunningState
import com.goide.execution.extension.GoRunConfigurationExtensionsManager
import com.goide.i18n.GoBundle
import com.goide.util.GoCommandLineParameter
import com.goide.util.GoExecutor
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.module.Module
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.coroutines.BspCoroutineService
import org.jetbrains.plugins.bsp.impl.server.connection.connection
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.taskEvents.BspTaskEventsService
import org.jetbrains.plugins.bsp.taskEvents.BspTaskListener
import org.jetbrains.plugins.bsp.taskEvents.OriginId


abstract class GoDebuggableCommandLineState(
  val environment: ExecutionEnvironment, module: Module, configuration: GoApplicationConfiguration, protected val originId: OriginId
) : GoApplicationRunningState(
  environment, module,
  configuration,
) {

  /** Run the actual BSP command or throw an exception if the server does not support running the configuration */
  protected abstract suspend fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities)

  override fun isDebug(): Boolean = true

  override fun getBuildingTarget(): List<GoCommandLineParameter>? = null

  override fun createBuildExecutor(): GoExecutor? = null

  protected abstract fun createAndAddTaskListener(handler: BspProcessHandler): BspTaskListener

  final override fun startProcess(): BspProcessHandler {
    val configuration = environment.runProfile as BspRunConfiguration
    val project = configuration.project

    val bspCoroutineService = BspCoroutineService.getInstance(project)

    lateinit var handler: BspProcessHandler
    // We have to start runDeferred later, because we need to register the listener first
    // Otherwise, we might miss some events
    val runDeferred = bspCoroutineService.startAsync(lazy = true) {
      project.connection.runWithServer { server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities ->
        withContext(Dispatchers.EDT) {
          RunContentManager.getInstance(project).toFrontRunContent(environment.executor, handler)
        }
        startBsp(server, capabilities)
      }
    }

    handler = BspProcessHandler(runDeferred)

    handler.addProcessListener(runtimeErrorsListener)

    ProcessTerminatedListener.attach(
      handler, myConfiguration.project,
      "\n" + GoBundle.message("go.execution.process.finished.with.exit.code", getProcessName(), "\$EXIT_CODE$") + "\n",
    )

    GoRunConfigurationExtensionsManager.getInstance().attachExtensionsToProcess(myConfiguration, handler, myEnvironment.runnerSettings)

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