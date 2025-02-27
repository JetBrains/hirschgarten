package org.jetbrains.bazel.run.commandLine

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import kotlinx.coroutines.future.await
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.run.BspCommandLineStateBase
import org.jetbrains.bazel.run.BspProcessHandler
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bazel.run.task.BspTestTaskListener
import org.jetbrains.bazel.taskEvents.BspTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.TestParams

class BspTestCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val state: GenericTestState,
) : BspCommandLineStateBase(environment, originId) {
  private val configuration = environment.runProfile as BspRunConfiguration

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override fun createAndAddTaskListener(handler: BspProcessHandler): BspTaskListener = BspTestTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer) {
    if (configuration.targets.isEmpty()) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val targets = configuration.targets
    val params =
      TestParams(
        targets,
        originId = originId,
        workingDirectory = state.workingDirectory,
        arguments = transformProgramArguments(state.programArguments),
        environmentVariables = state.env.envs,
        coverage = false,
        testFilter = state.testFilter,
        additionalBazelParams = state.additionalBazelParams,
      )
    // TODO: handle coverage (BAZEL-1364)
    server.buildTargetTest(params).await()
  }
}
