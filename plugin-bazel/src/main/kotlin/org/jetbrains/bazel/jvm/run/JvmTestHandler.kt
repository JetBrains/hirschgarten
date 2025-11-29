package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.util.Ref
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.run.task.JetBrainsTestRunnerTaskListener
import org.jetbrains.bazel.run.test.useJetBrainsTestRunner
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JoinedBuildServer

class JvmTestHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    // KotlinCoroutineLibraryFinderBeforeRunTaskProvider must be run before BuildScriptBeforeRunTaskProvider
    configuration.setBeforeRunTasksFromHandler(
      listOfNotNull(
        KotlinCoroutineLibraryFinderBeforeRunTaskProvider().createTask(configuration),
        ScriptPathBeforeRunTaskProvider().createTask(configuration),
      ),
    )
  }

  override val name: String
    get() = "Jvm Test Handler"

  override val state = JvmTestState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    if (executor is DefaultDebugExecutor) {
      // Only pass SCRIPT_PATH_KEY for debug, because it screws up test result caching; see ScriptPathBeforeRunTaskProvider
      environment.putCopyableUserData(SCRIPT_PATH_KEY, Ref())
      environment.putCopyableUserData(COROUTINE_JVM_FLAGS_KEY, Ref())
      return ScriptPathTestCommandLineState(environment, state)
    }
    return BazelTestCommandLineState(environment, state)
  }

  class JvmTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "JvmTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JvmTestHandler(configuration)

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      targetInfos.all {
        (it.kind.isJvmTarget() && it.kind.ruleType == RuleType.TEST)
      }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = targetInfos.size == 1 && canRun(targetInfos)

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = true
  }
}

class ScriptPathTestCommandLineState(environment: ExecutionEnvironment, val settings: JvmTestState) :
  JvmDebuggableCommandLineState(environment, settings.debugPort) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener =
    if (environment.project.useJetBrainsTestRunner()) {
      JetBrainsTestRunnerTaskListener(handler)
    } else {
      BazelTestTaskListener(handler)
    }

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override suspend fun startBsp(
    server: JoinedBuildServer,
    pidDeferred: CompletableDeferred<Long?>,
    handler: BazelProcessHandler,
  ) {
    val scriptPath = checkNotNull(environment.getCopyableUserData(SCRIPT_PATH_KEY)?.get()) { "Missing --script_path" }
    runWithScriptPath(scriptPath, environment.project, originId, pidDeferred, handler, settings.env.envs)
  }
}
