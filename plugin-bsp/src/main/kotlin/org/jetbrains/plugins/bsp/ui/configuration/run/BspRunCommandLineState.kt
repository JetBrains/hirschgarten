package org.jetbrains.plugins.bsp.ui.configuration.run

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.connection.BspServer
import org.jetbrains.plugins.bsp.server.connection.connection
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.services.BspTaskListener
import org.jetbrains.plugins.bsp.services.OriginId
import org.jetbrains.plugins.bsp.ui.configuration.BspProcessHandler
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.BspRunConfigurationBase
import java.util.concurrent.CompletableFuture

public abstract class BspCommandLineStateBase(
  private val project: Project,
  private val environment: ExecutionEnvironment,
  private val configuration: BspRunConfigurationBase,
  private val originId: OriginId,
) : CommandLineState(environment) {
  protected abstract fun checkRunCapabilities(capabilities: BazelBuildServerCapabilities)

  protected abstract fun createAndAddTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener

  protected abstract fun startBsp(server: BspServer): CompletableFuture<*>

  final override fun startProcess(): BspProcessHandler<out Any> {
    // We have to start runFuture later, because we need to register the listener first
    // Otherwise, we might miss some events
    val computationStarter = CompletableFuture<Unit>()
    val runFuture = computationStarter.thenCompose {
      val completableFuture: CompletableFuture<*> = project.connection.runWithServer { server, capabilities ->
        checkRunCapabilities(capabilities)
        startBsp(server)
      }
      // The above "useless" type is actually needed because of a bug in Kotlin compiler
      completableFuture
    }

    val handler = BspProcessHandler(runFuture)
    val runListener = createAndAddTaskListener(handler)

    with(BspTaskEventsService.getInstance(project)) {
      saveListener(originId, runListener)
      runFuture.handle { _, _ ->
        removeListener(originId)
      }
    }

    computationStarter.complete(Unit)
    handler.startNotify()

    return handler
  }
}

internal class BspRunCommandLineState(
  project: Project,
  environment: ExecutionEnvironment,
  private val configuration: BspRunConfiguration,
  private val originId: OriginId,
) : BspCommandLineStateBase(project, environment, configuration, originId) {
  override fun checkRunCapabilities(capabilities: BazelBuildServerCapabilities) {
    if (configuration.targets.singleOrNull()?.id == null || capabilities.runProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }
  }

  override fun createAndAddTaskListener(handler: BspProcessHandler<out Any>): BspTaskListener =
    BspRunTaskListener(handler)

  override fun startBsp(server: BspServer): CompletableFuture<*> {
    // SAFETY: safe to unwrap because we checked in checkRunCapabilities
    val targetId = BuildTargetIdentifier(configuration.targets.single().id)
    val runParams = RunParams(targetId)
    runParams.originId = originId
    return server.buildTargetRun(runParams)
  }
}
