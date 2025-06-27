package org.jetbrains.bazel.golang.debug

import com.goide.execution.application.GoApplicationConfiguration
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.includesGo
import org.jetbrains.bazel.sync.projectStructure.legacy.MonoModuleUtils
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.DebugType
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.TestParams
import java.util.UUID

class BazelGoTestHandler(private val configuration: BazelRunConfiguration) : BazelRunHandler {
  override val name: String = "Bazel Go Test Handler"

  override val state = GenericTestState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        val config = GoApplicationConfiguration(environment.project, "default", BazelRunConfigurationType())
        val target = getTargetId(environment)
        val module = MonoModuleUtils.findModule(environment.project) ?: error("Could not find module for target $target")
        GoTestWithDebugCommandLineState(environment, UUID.randomUUID().toString(), module, config, state)
      }

      else -> {
        BazelTestCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  private fun getTargetId(environment: ExecutionEnvironment): Label =
    (environment.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()
      ?: throw ExecutionException("Couldn't get BSP target from run configuration")

  class GoBazelRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String = "GoBspRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelGoTestHandler = BazelGoTestHandler(configuration)

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      BazelFeatureFlags.isGoSupportEnabled && targetInfos.all { it.kind.includesGo() && it.kind.ruleType == RuleType.TEST }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazeGoRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}

class GoTestWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  module: Module,
  configuration: GoApplicationConfiguration,
  val settings: GenericTestState,
) : GoDebuggableCommandLineState(environment, module, configuration, originId) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer) {
    val configuration = environment.runProfile as BazelRunConfiguration
    val testParams =
      TestParams(
        targets = configuration.targets,
        originId = originId,
        workingDirectory = settings.workingDirectory,
        arguments = transformProgramArguments(settings.programArguments),
        environmentVariables = settings.env.envs,
        debug = DebugType.GoDlv(debugServerAddress.port),
        testFilter = settings.testFilter,
      )

    server.buildTargetTest(testParams)
  }
}
