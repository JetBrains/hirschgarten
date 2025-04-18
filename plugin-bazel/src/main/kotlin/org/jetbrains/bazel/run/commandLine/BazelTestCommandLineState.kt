package org.jetbrains.bazel.run.commandLine

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericTestState
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.TestParams
import java.nio.file.Path

class BazelTestCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val state: AbstractGenericTestState<*>,
) : BazelCommandLineStateBase(environment, originId) {
  var coverageReportListener: ((Path) -> Unit)? = null

  private val configuration = environment.runProfile as BazelRunConfiguration

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener =
    BazelTestTaskListener(handler, coverageReportListener)

  override suspend fun startBsp(server: JoinedBuildServer) {
    if (configuration.targets.isEmpty()) {
      throw ExecutionException(BazelPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val targets = configuration.targets
    val params =
      TestParams(
        targets,
        originId = originId,
        workingDirectory = state.workingDirectory,
        arguments = transformProgramArguments(state.programArguments),
        environmentVariables = state.env.envs,
        coverage = environment.executor.id == CoverageExecutor.EXECUTOR_ID,
        testFilter = state.testFilter,
        additionalBazelParams = state.additionalBazelParams,
      )
    server.buildTargetTest(params)
  }
}
