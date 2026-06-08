package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.util.Ref
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.run.task.JetBrainsTestRunnerTaskListener
import org.jetbrains.bazel.run.test.useJetBrainsTestRunner
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bazel.server.BazelServerFacade

@ApiStatus.Internal
class JvmTestHandler(private val configuration: BazelRunConfiguration) : BazelRunHandler {
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

  override val isTestHandler: Boolean = true

  override val state = JvmTestState(configuration.project)

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    if (executor is DefaultDebugExecutor) {
      environment.putCopyableUserData(COROUTINE_JVM_FLAGS_KEY, Ref())
    }
    return if (RunWithScriptPathExtension.shouldRunWithScriptPath(executor, configuration)) {
      environment.putCopyableUserData(SCRIPT_PATH_KEY, Ref())
      ScriptPathTestCommandLineState(environment, state, configuration)
    }
    else {
      BazelTestCommandLineState(environment, state)
    }
  }

  override val extensionsManager: RunConfigurationExtensionsManager<in RunConfigurationBase<*>, *>
    get() = JavaRunConfigurationExtensionManager.instance

  class JvmTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "JvmTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JvmTestHandler(configuration)

    override fun canRun(targets: List<TargetKind>): Boolean =
      targets.all {
        (it.isJvmTarget() && it.ruleType == RuleType.TEST)
      }

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = true
  }
}

internal class ScriptPathTestCommandLineState(
  environment: ExecutionEnvironment,
  val settings: JvmTestState,
  configuration: BazelRunConfiguration,
) :
  JvmDebuggableCommandLineState(environment, settings.debugPort, configuration) {
  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener =
    if (environment.project.useJetBrainsTestRunner()) {
      JetBrainsTestRunnerTaskListener(handler)
    } else {
      BazelTestTaskListener(handler)
    }

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override suspend fun startBsp(
      server: BazelServerFacade,
      pidDeferred: CompletableDeferred<Long?>,
      handler: BazelProcessHandler,
  ) {
    val scriptPath = checkNotNull(environment.getCopyableUserData(SCRIPT_PATH_KEY)?.get()) { "Missing --script_path" }
    runWithScriptPath(
      taskGroupId.task("jvm-test"),
      scriptPath = scriptPath,
      project = environment.project,
      pidDeferred = pidDeferred,
      handler = handler,
      env = settings.env.envs,
      additionalScriptParameters = getAdditionalJvmRunParameters(environment, settings.debugPort),
      isTest = true,
      testFilter = settings.testFilter,
    ) { processHandler ->
      attachJvmRunExtensions(environment, processHandler)
    }
  }
}
