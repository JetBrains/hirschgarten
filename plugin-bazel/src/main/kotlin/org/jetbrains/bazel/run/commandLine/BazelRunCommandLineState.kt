package org.jetbrains.bazel.run.commandLine

import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericRunState
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RunParams

class BazelRunCommandLineState(environment: ExecutionEnvironment, private val runState: AbstractGenericRunState<*>) :
  BazelCommandLineStateBase(environment) {
  private val configuration = BazelRunConfiguration.get(environment)

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(
    server: JoinedBuildServer,
    pidDeferred: CompletableDeferred<Long?>,
    handler: BazelProcessHandler,
  ) {
    if (configuration.targets.singleOrNull() == null) {
      throw ExecutionException(BazelPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val runParams =
      RunParams(
        target = configuration.targets.single(),
        originId = originId.toString(),
        arguments = transformProgramArguments(runState.programArguments),
        environmentVariables = runState.env.envs,
        workingDirectory = runState.workingDirectory,
        additionalBazelParams = runState.additionalBazelParams,
        pidDeferred = pidDeferred,
      )
    server.buildTargetRun(runParams)
  }
}
