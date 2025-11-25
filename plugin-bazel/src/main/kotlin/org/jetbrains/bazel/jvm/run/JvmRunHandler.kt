package org.jetbrains.bazel.jvm.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.commandLine.transformProgramArguments
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JoinedBuildServer
import org.jetbrains.bsp.protocol.RunParams
import org.jetbrains.bsp.protocol.RunWithDebugParams
import java.util.concurrent.atomic.AtomicReference

internal val COROUTINE_JVM_FLAGS_KEY = Key.create<AtomicReference<List<String>>>("bazel.coroutine.jvm.flags")

class JvmRunHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    // KotlinCoroutineLibraryFinderBeforeRunTaskProvider must be run before HotSwapRunBeforeRunTaskProvider
    configuration.beforeRunTasks =
      listOfNotNull(
        KotlinCoroutineLibraryFinderBeforeRunTaskProvider().createTask(configuration),
        HotSwapRunBeforeRunTaskProvider().createTask(configuration),
      )
  }

  override val name: String
    get() = "Jvm Run Handler"

  override val state = JvmRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        environment.putCopyableUserData(SCRIPT_PATH_KEY, AtomicReference())
        environment.putCopyableUserData(COROUTINE_JVM_FLAGS_KEY, AtomicReference())
        JvmRunWithDebugCommandLineState(environment, state)
      }

      else -> {
        BazelRunCommandLineState(environment, state)
      }
    }

  class JvmRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "JvmRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JvmRunHandler(configuration)

    // Explanation for this logic:
    // Because we have android_local_test with mocked Android classes, which should be run, well, locally,
    //  as opposed to on-device like with android_binary
    // TODO: perhaps better solved by having a tag
    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      targetInfos.all {
        it.kind.isJvmTarget() && it.kind.ruleType != RuleType.TEST
      }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}

class JvmRunWithDebugCommandLineState(environment: ExecutionEnvironment, val settings: JvmRunState) :
  JvmDebuggableCommandLineState(environment, settings.debugPort) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(
    server: JoinedBuildServer,
    pidDeferred: CompletableDeferred<Long?>,
    handler: BazelProcessHandler,
  ) {
    val scriptPath = environment.getCopyableUserData(SCRIPT_PATH_KEY)?.get()
    if (scriptPath != null) {
      debugWithScriptPath(scriptPath.toString(), pidDeferred, handler, settings.env.envs)
    } else {
      val configuration = BazelRunConfiguration.get(environment)
      val kotlinCoroutineLibParam = retrieveKotlinCoroutineParams(environment, configuration.project).joinToString(" ")
      val additionalBazelParams = settings.additionalBazelParams ?: ""
      val runParams =
        RunParams(
          target = configuration.targets.single(),
          originId = originId.toString(),
          arguments = transformProgramArguments(settings.programArguments),
          environmentVariables = settings.env.envs,
          additionalBazelParams = (additionalBazelParams + kotlinCoroutineLibParam).trim().ifEmpty { null },
          pidDeferred = pidDeferred,
        )
      val runWithDebugParams =
        RunWithDebugParams(
          originId = originId.toString(),
          runParams = runParams,
          debug = debugType,
        )

      server.buildTargetRunWithDebug(runWithDebugParams)
    }
  }
}
