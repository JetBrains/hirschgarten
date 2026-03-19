package org.jetbrains.bazel.jvm.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.BazelServerFacade

internal val COROUTINE_JVM_FLAGS_KEY = Key.create<Ref<List<String>>>("bazel.coroutine.jvm.flags")

internal class JvmRunHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
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
    get() = "Jvm Run Handler"

  override val isTestHandler: Boolean = false

  override val state = JvmRunState(configuration.project)

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    if (executor is DefaultDebugExecutor) {
      environment.putCopyableUserData(COROUTINE_JVM_FLAGS_KEY, Ref())
    }
    return if (state.runWithBazel && executor is DefaultRunExecutor) {
      BazelRunCommandLineState(environment, state)
    }
    else {
      environment.putCopyableUserData(SCRIPT_PATH_KEY, Ref())
      RunScriptPathCommandLineState(environment, state)
    }
  }

  class JvmRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "JvmRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JvmRunHandler(configuration)

    override fun canRun(targets: List<TargetKind>): Boolean =
      targets.all {
        it.isJvmTarget() && it.ruleType != RuleType.TEST
      }

    override fun canDebug(targets: List<TargetKind>): Boolean = canRun(targets)

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}

internal class RunScriptPathCommandLineState(environment: ExecutionEnvironment, val settings: JvmRunState) :
  JvmDebuggableCommandLineState(environment, settings.debugPort) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = BazelRunTaskListener(handler)

  override suspend fun startBsp(
      server: BazelServerFacade,
      pidDeferred: CompletableDeferred<Long?>,
      handler: BazelProcessHandler,
  ) {
    val scriptPath = checkNotNull(environment.getCopyableUserData(SCRIPT_PATH_KEY)?.get()) { "Missing --script_path" }
    runWithScriptPath(
      taskGroupId.task("jvm-run"),
      scriptPath,
      environment.project,
      pidDeferred,
      handler,
      settings.env.envs,
      isTest = false,
      testFilter = null,
    )
  }
}
