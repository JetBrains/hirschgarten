package org.jetbrains.plugins.bsp.run.commandLine

import ch.epfl.scala.bsp4j.TestParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.BazelTestParamsData
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.run.BspCommandLineStateBase
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericTestState
import org.jetbrains.plugins.bsp.run.task.BspTestTaskListener
import org.jetbrains.plugins.bsp.taskEvents.BspTaskListener
import org.jetbrains.plugins.bsp.taskEvents.OriginId

class BspTestCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val state: GenericTestState,
) : BspCommandLineStateBase(environment, originId) {
  private val configuration = environment.runProfile as BspRunConfiguration

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override fun createAndAddTaskListener(handler: BspProcessHandler): BspTaskListener = BspTestTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities) {
    if (configuration.targets.isEmpty() || capabilities.testProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val targets = configuration.targets
    val params = TestParams(targets)
    params.originId = originId
    params.workingDirectory = state.workingDirectory
    params.arguments = transformProgramArguments(state.programArguments)
    params.environmentVariables = state.env.envs
    params.dataKind = BazelTestParamsData.DATA_KIND
    params.data =
      BazelTestParamsData(
        coverage = false,
        testFilter = state.testFilter,
        additionalBazelParams = state.additionalBazelParams,
      )
    // TODO: handle coverage (BAZEL-1364)
    server.buildTargetTest(params).await()
  }
}
