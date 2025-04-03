package org.jetbrains.bazel.run.commandLine

import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RunParams

class BazelRunCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  private val runState: GenericRunState,
) : BazelCommandLineStateBase(environment, originId) {
  private val configuration = environment.runProfile as BazelRunConfiguration

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer) {
    if (configuration.targets.singleOrNull() == null) {
      throw ExecutionException(BazelPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val targetId = configuration.targets.single()
    val runParams =
      RunParams(
        targetId,
        originId = originId,
        arguments = transformProgramArguments(runState.programArguments),
        environmentVariables = runState.env.envs,
        workingDirectory = runState.workingDirectory,
        additionalBazelParams = runState.additionalBazelParams,
      )
    server.buildTargetRun(runParams)
  }
}
