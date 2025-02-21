package org.jetbrains.bazel.golang.debug

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
import org.jetbrains.bazel.assets.assets
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.run.BspProcessHandler
import org.jetbrains.bazel.run.BspRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.commandLine.BspRunCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.run.config.BspRunConfigurationType
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.task.BspRunTaskListener
import org.jetbrains.bazel.target.getModule
import org.jetbrains.bazel.taskEvents.BspTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.includesGo
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.RunWithDebugParams
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
        val module = target.label().getModule(environment.project) ?: error("Could not find module for target $target")
        GoRunWithDebugCommandLineState(environment, UUID.randomUUID().toString(), module, config, state)
      }

      else -> {
        BspRunCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  private fun getTargetId(environment: ExecutionEnvironment): BuildTargetIdentifier =
    (environment.runProfile as? BspRunConfiguration)?.targets?.singleOrNull()
      ?: throw ExecutionException("Couldn't get BSP target from run configuration")

  class GoBspRunHandlerProvider : RunHandlerProvider {
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
