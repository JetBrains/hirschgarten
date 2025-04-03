package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bazel.workspacemodel.entities.isJvmTarget
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.TestParams
import java.util.UUID

class JvmTestHandler : BazelRunHandler {
  override val name: String = "Jvm BSP Test Handler"

  override val state = GenericTestState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        JvmTestWithDebugCommandLineState(environment, UUID.randomUUID().toString(), state)
      }

      else -> {
        BazelTestCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  class JvmTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String = "JvmBspTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JvmTestHandler()

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      targetInfos.all {
        (it.languageIds.isJvmTarget() && it.capabilities.canTest)
      }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = true
  }
}

class JvmTestWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val settings: GenericTestState,
) : JvmDebuggableCommandLineState(environment, originId) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelTestTaskListener(handler)

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override suspend fun startBsp(server: JoinedBuildServer) {
    val configuration = environment.runProfile as BazelRunConfiguration
    val targetIds = configuration.targets
    val testParams =
      TestParams(
        targets = targetIds,
        originId = originId,
        workingDirectory = settings.workingDirectory,
        arguments = transformProgramArguments(settings.programArguments),
        environmentVariables = settings.env.envs,
        debug = RemoteDebugData("jdwp", getConnectionPort()),
        testFilter = settings.testFilter,
      )

    server.buildTargetTest(testParams)
  }
}
