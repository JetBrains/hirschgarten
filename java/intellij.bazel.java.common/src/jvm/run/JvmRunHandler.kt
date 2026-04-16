package org.jetbrains.bazel.jvm.run

import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.annotations.ApiStatus
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

@ApiStatus.Internal
class JvmRunHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
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
      additionalScriptParameters = getAdditionalJvmRunParameters(environment, settings.debugPort),
      isTest = false,
      testFilter = null,
    ) { processHandler ->
      attachJvmRunExtensions(environment, processHandler)
    }
  }
}

internal fun getAdditionalJvmRunParameters(environment: ExecutionEnvironment, debugPort: Int): List<String> = buildList {
  val configuration = environment.runProfile as? BazelRunConfiguration ?: return@buildList

  if (environment.executor is DefaultDebugExecutor) {
    // https://bazel.build/reference/command-line-reference#flag--java_debug
    // https://github.com/bazelbuild/rules_java/blob/747bddd6091a624c54a42c1ac20308190c1ad849/java/bazel/rules/java_stub_template.txt#L23
    this += "--wrapper_script_flag=--debug=$debugPort"
    this += retrieveKotlinCoroutineParams(environment, environment.project).map {
      wrapVmOptionAsArg(it)
    }
  }

  val profilerParameters = JavaParameters()
  // JavaRunConfigurationExtensionManager has a generic-sounding name, but in practice only used for JFR/Async Profiler VM options
  JavaRunConfigurationExtensionManager.instance.updateJavaParameters(
    configuration,
    profilerParameters,
    environment.runnerSettings,
    environment.executor,
  )
  this += profilerParameters.vmParametersList.parameters.map { wrapVmOptionAsArg(it) }
}

private fun wrapVmOptionAsArg(vmOption: String): String {
  // https://github.com/bazelbuild/rules_java/blob/747bddd6091a624c54a42c1ac20308190c1ad849/java/bazel/rules/java_stub_template.txt#L33
  return "--wrapper_script_flag=--jvm_flag=$vmOption"
}

internal fun attachJvmRunExtensions(environment: ExecutionEnvironment, processHandler: OSProcessHandler) {
  val configuration = environment.runProfile as? BazelRunConfiguration ?: return
  JavaRunConfigurationExtensionManager.instance.attachExtensionsToProcess(configuration, processHandler, environment.runnerSettings)
}
