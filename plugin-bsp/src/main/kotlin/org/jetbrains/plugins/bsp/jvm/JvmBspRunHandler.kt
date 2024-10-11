package org.jetbrains.plugins.bsp.jvm

import ch.epfl.scala.bsp4j.RunParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.RunWithDebugParams
import org.jetbrains.plugins.bsp.run.BspCommandLineStateBase
import org.jetbrains.plugins.bsp.run.BspProcessHandler
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.commandLine.BspRunCommandLineState
import org.jetbrains.plugins.bsp.run.commandLine.transformProgramArguments
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericRunState
import org.jetbrains.plugins.bsp.run.task.BspRunTaskListener
import org.jetbrains.plugins.bsp.taskEvents.BspTaskListener
import org.jetbrains.plugins.bsp.taskEvents.OriginId
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.includesAndroid
import org.jetbrains.plugins.bsp.workspacemodel.entities.isJvmTarget
import java.util.UUID

class JvmBspRunHandler(private val configuration: BspRunConfiguration) : BspRunHandler {
  override val name: String = "Jvm BSP Run Handler"

  override val state = GenericRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        JvmDebugHandlerState(environment, UUID.randomUUID().toString(), state)
      }

      else -> {
        BspRunCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  class JvmBspRunHandlerProvider : BspRunHandlerProvider {
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

class JvmDebugHandlerState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val settings: GenericRunState,
) : BspCommandLineStateBase(environment, originId) {
  val remoteConnection: RemoteConnection =
    RemoteConnection(true, "localhost", "0", true)

  private val portForDebug: Int?
    get() = remoteConnection.debuggerAddress?.toInt()

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
    val remoteDebugData = RemoteDebugData("jdwp", portForDebug!!)
    val runWithDebugParams = RunWithDebugParams(originId, runParams, remoteDebugData)

    server.buildTargetRunWithDebug(runWithDebugParams).await()
  }
}
