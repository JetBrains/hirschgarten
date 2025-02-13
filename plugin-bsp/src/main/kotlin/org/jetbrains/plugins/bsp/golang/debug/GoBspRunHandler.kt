package org.jetbrains.plugins.bsp.golang.debug

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.RunParams
import com.goide.execution.application.GoApplicationConfiguration
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.plugins.bsp.assets.assets
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.commandLine.BspRunCommandLineState
import org.jetbrains.plugins.bsp.run.commandLine.transformProgramArguments
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.config.BspRunConfigurationType
import org.jetbrains.plugins.bsp.run.state.GenericRunState
import org.jetbrains.plugins.bsp.run.task.BspRunTaskListener
import org.jetbrains.plugins.bsp.target.getModule
import org.jetbrains.plugins.bsp.taskEvents.BspTaskListener
import org.jetbrains.plugins.bsp.taskEvents.OriginId
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.includesGo
import java.util.UUID

class GoBspRunHandler(private val configuration: BspRunConfiguration) : BspRunHandler {
  private val buildToolName: String = configuration.project.assets.presentableName
  override val name: String = "Go $buildToolName Run Handler"

  override val state = GenericRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        val config = GoApplicationConfiguration(environment.project, "default", BspRunConfigurationType())
        val target = getTargetId(environment)
        val module = target.getModule(environment.project) ?: error("Could not find module for target $target")
        GoRunWithDebugCommandLineState(environment, UUID.randomUUID().toString(), module, config, state)
      }

      else -> {
        BspRunCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  private fun getTargetId(environment: ExecutionEnvironment): BuildTargetIdentifier =
    (environment.runProfile as? BspRunConfiguration)?.targets?.singleOrNull()
      ?: throw ExecutionException("Couldn't get BSP target from run configuration")

  class GoBspRunHandlerProvider : BspRunHandlerProvider {
    override val id: String = "GoBspRunHandlerProvider"

    override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = GoBspRunHandler(configuration)

    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      BspFeatureFlags.isGoSupportEnabled && targetInfos.all { it.languageIds.includesGo() }

    override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = canRun(targetInfos)
  }
}

class GoRunWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  module: Module,
  configuration: GoApplicationConfiguration,
  val settings: GenericRunState,
) : GoDebuggableCommandLineState(environment, module, configuration, originId) {
  override fun createAndAddTaskListener(handler: BspProcessHandler): BspTaskListener = BspRunTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities) {
    if (!capabilities.runWithDebugProvider) {
      throw ExecutionException("BSP server does not support running")
    }

    val configuration = environment.runProfile as BspRunConfiguration
    val targetId = configuration.targets.single()
    val runParams = RunParams(targetId)
    runParams.originId = originId
    runParams.workingDirectory = settings.workingDirectory
    runParams.arguments = transformProgramArguments(settings.programArguments)
    runParams.environmentVariables = settings.env.envs
    val remoteDebugData = RemoteDebugData("go_dlv", getDebugServerAddress().port)
    val runWithDebugParams = RunWithDebugParams(originId, runParams, remoteDebugData)

    server.buildTargetRunWithDebug(runWithDebugParams).await()
  }
}
