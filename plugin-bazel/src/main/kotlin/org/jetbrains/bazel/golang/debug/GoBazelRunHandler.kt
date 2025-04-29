package org.jetbrains.bazel.golang.debug

import com.goide.execution.application.GoApplicationConfiguration
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.target.getModule
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bazel.workspacemodel.entities.includesGo
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.DebugType
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunWithDebugParams
import java.util.UUID

class GoBazelRunHandler(private val configuration: BazelRunConfiguration) : BazelRunHandler {
  private val buildToolName: String = BazelPluginConstants.BAZEL_DISPLAY_NAME
  override val name: String = "Go $buildToolName Run Handler"

  override val state = GenericRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        val config = GoApplicationConfiguration(environment.project, "default", BazelRunConfigurationType())
        val target = getTargetId(environment)
        val module = target.getModule(environment.project) ?: error("Could not find module for target $target")
        GoRunWithDebugCommandLineState(environment, UUID.randomUUID().toString(), module, config, state)
      }

      else -> {
        BazelRunCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  private fun getTargetId(environment: ExecutionEnvironment): Label =
    (environment.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()
      ?: throw ExecutionException("Couldn't get BSP target from run configuration")

  class GoBazelRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String = "GoBspRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = GoBazelRunHandler(configuration)

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      BazelFeatureFlags.isGoSupportEnabled && targetInfos.all { it.kind.includesGo() }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazeGoRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}

class GoRunWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  module: Module,
  configuration: GoApplicationConfiguration,
  val settings: GenericRunState,
) : GoDebuggableCommandLineState(environment, module, configuration, originId) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer) {
    val configuration = environment.runProfile as BazelRunConfiguration
    val targetId = configuration.targets.single()
    val runParams =
      RunParams(
        targetId,
        originId = originId,
        workingDirectory = settings.workingDirectory,
        arguments = transformProgramArguments(settings.programArguments),
        environmentVariables = settings.env.envs,
        additionalBazelParams = settings.additionalBazelParams,
      )
    val remoteDebugData = DebugType.GoDlv(debugServerAddress.port)
    val runWithDebugParams = RunWithDebugParams(originId, runParams, remoteDebugData)

    server.buildTargetRunWithDebug(runWithDebugParams)
  }
}
