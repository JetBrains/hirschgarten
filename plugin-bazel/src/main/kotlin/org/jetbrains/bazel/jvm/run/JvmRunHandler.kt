package org.jetbrains.bazel.jvm.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.DebugType
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunWithDebugParams
import java.util.UUID

class JvmRunHandler(val configuration: BazelRunConfiguration) : BazelRunHandler {
  private val buildToolName: String = BazelPluginConstants.BAZEL_DISPLAY_NAME
  override val name: String = "Jvm $buildToolName Run Handler"

  override val state = JvmRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        JvmRunWithDebugCommandLineState(environment, UUID.randomUUID().toString(), state)
      }

      else -> {
        BazelRunCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  class JvmRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String = "JvmBspRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JvmRunHandler(configuration)

    // Explanation for this logic:
    // Because we have android_local_test with mocked Android classes, which should be run, well, locally,
    //  as opposed to on-device like with android_binary
    // TODO: perhaps better solved by having a tag
    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      targetInfos.all {
        (it.kind.isJvmTarget() && it.kind.ruleType != RuleType.TEST) ||
          (it.kind.includesAndroid() && it.kind.ruleType == RuleType.TEST)
      }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}

class JvmRunWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  val settings: JvmRunState,
) : JvmDebuggableCommandLineState(environment, originId, settings.debugPort) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(server: JoinedBuildServer, pidDeferred: CompletableDeferred<Long?>) {
    val configuration = environment.runProfile as BazelRunConfiguration
    val targetId = configuration.targets.single()
    val runParams =
      RunParams(
        targetId,
        originId = originId,
        arguments = transformProgramArguments(settings.programArguments),
        environmentVariables = settings.env.envs,
        workingDirectory = settings.workingDirectory,
        additionalBazelParams = settings.additionalBazelParams,
        pidDeferred = pidDeferred,
      )
    val remoteDebugData = DebugType.JDWP(getConnectionPort())
    val runWithDebugParams = RunWithDebugParams(originId, runParams, remoteDebugData)

    server.buildTargetRunWithDebug(runWithDebugParams)
  }
}
