package org.jetbrains.bazel.run.commandLine

import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.runners.ExecutionEnvironment
import kotlinx.coroutines.future.await
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.run.BspCommandLineStateBase
import org.jetbrains.bazel.run.BspProcessHandler
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.task.BspRunTaskListener
import org.jetbrains.bazel.taskEvents.BspTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer

class BspRunCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  private val runState: GenericRunState,
) : BspCommandLineStateBase(environment, originId) {
  private val configuration = environment.runProfile as BspRunConfiguration

  override fun createAndAddTaskListener(handler: BspProcessHandler): BspTaskListener = BspRunTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities) {
    if (configuration.targets.singleOrNull() == null || capabilities.runProvider == null) {
      throw ExecutionException(BspPluginBundle.message("bsp.run.error.cannotRun"))
    }

    val targetId = configuration.targets.single()
    val runParams = RunParams(targetId)
    runParams.originId = originId
    runParams.arguments = transformProgramArguments(runState.programArguments)
    runParams.environmentVariables = runState.env.envs
    runParams.workingDirectory = runState.workingDirectory
    server.buildTargetRun(runParams).await()
  }
}
