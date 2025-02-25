package org.jetbrains.bazel.jvm.run

import ch.epfl.scala.bsp4j.TestParams
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import kotlinx.coroutines.future.asDeferred
import org.jetbrains.bazel.run.BspProcessHandler
import org.jetbrains.bazel.run.BspRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.commandLine.BspTestCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bazel.run.task.BspTestTaskListener
import org.jetbrains.bazel.taskEvents.BspTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.isJvmTarget
import org.jetbrains.bsp.protocol.BazelBuildServerCapabilities
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RemoteDebugData
import org.jetbrains.bsp.protocol.TestWithDebugParams
import java.util.UUID

class JvmBspTestHandler : BspRunHandler {
  override val name: String = "Jvm BSP Test Handler"

  override val state = GenericTestState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        JvmTestWithDebugCommandLineState(environment, UUID.randomUUID().toString(), state)
      }

      else -> {
        BspTestCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  class JvmBspTestHandlerProvider : RunHandlerProvider {
    override val id: String = "JvmBspTestHandlerProvider"

    override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = JvmBspTestHandler()

    override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
      targetInfos.all {
        (it.languageIds.isJvmTarget() && it.capabilities.canTest)
      }

    override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = canRun(targetInfos)
  }
}

class JvmTestWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val settings: GenericTestState,
) : JvmDebuggableCommandLineState(environment, originId) {
  override fun createAndAddTaskListener(handler: BspProcessHandler): BspTaskListener = BspTestTaskListener(handler)

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override suspend fun startBsp(server: JoinedBuildServer, capabilities: BazelBuildServerCapabilities) {
    if (!capabilities.testWithDebugProvider) {
      throw ExecutionException("BSP server does not support testing with debugging")
    }

    val configuration = environment.runProfile as BspRunConfiguration
    val targetIds = configuration.targets
    val testParams = TestParams(targetIds)
    testParams.originId = originId
    testParams.workingDirectory = settings.workingDirectory
    testParams.arguments = transformProgramArguments(settings.programArguments)
    testParams.environmentVariables = settings.env.envs
    val remoteDebugData = RemoteDebugData("jdwp", getConnectionPort())
    val testWithDebugParams = TestWithDebugParams(originId, testParams, remoteDebugData)

    server.buildTargetTestWithDebug(testWithDebugParams).asDeferred().await()
  }
}
