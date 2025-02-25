package org.jetbrains.bazel.jvm.run

import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import kotlinx.coroutines.future.await
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.run.BspProcessHandler
import org.jetbrains.bazel.run.BspRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.commandLine.BspRunCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.task.BspRunTaskListener
import org.jetbrains.bazel.taskEvents.BspTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.includesAndroid
import org.jetbrains.bazel.workspacemodel.entities.isJvmTarget
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.RunWithDebugParams
import java.util.UUID

class JvmBspRunHandler(val configuration: BspRunConfiguration) : BspRunHandler {
  private val buildToolName: String = BazelPluginConstants.BAZEL_DISPLAY_NAME
  override val name: String = "Jvm $buildToolName Run Handler"

  override val state = GenericRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        JvmRunWithDebugCommandLineState(environment, UUID.randomUUID().toString(), state)
      }

      else -> {
        BspRunCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  class JvmBspRunHandlerProvider : RunHandlerProvider {
    override val id: String = "JvmBspRunHandlerProvider"

    override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = JvmBspRunHandler(configuration)

    // Explanation for this logic:
    // Because we have android_local_test with mocked Android classes, which should be run, well, locally,
    //  as opposed to on-device like with android_binary
    // TODO: perhaps better solved by having a tag
    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      targetInfos.all {
        (it.languageIds.isJvmTarget() && !it.capabilities.canTest) ||
          (it.languageIds.includesAndroid() && it.capabilities.canTest)
      }

    override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = canRun(targetInfos)
  }
}

class JvmRunWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val settings: GenericRunState,
) : JvmDebuggableCommandLineState(environment, originId) {
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
    val remoteDebugData = RemoteDebugData("jdwp", getConnectionPort())
    val runWithDebugParams = RunWithDebugParams(originId, runParams, remoteDebugData)

    server.buildTargetRunWithDebug(runWithDebugParams).await()
  }
}
