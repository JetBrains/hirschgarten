package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.engine.AsyncStacksUtils.addDebuggerAgent
import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.isJvmTarget
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
val COROUTINE_JVM_FLAGS_KEY: Key<Ref<List<String>>> = Key.create("bazel.coroutine.jvm.flags")

@ApiStatus.Internal
class JvmRunHandler(private val configuration: BazelRunConfiguration) : BazelRunHandler {
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
    return if (RunWithScriptPathExtension.shouldRunWithScriptPath(executor, configuration)) {
      environment.putCopyableUserData(SCRIPT_PATH_KEY, Ref())
      RunScriptPathCommandLineState(environment, state, configuration)
    }
    else {
      BazelRunCommandLineState(environment, state)
    }
  }

  override val extensionsManager: RunConfigurationExtensionsManager<in RunConfigurationBase<*>, *>
    get() = JavaRunConfigurationExtensionManager.instance

  class JvmRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "JvmRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JvmRunHandler(configuration)

    override fun canRun(project: Project, targets: List<BuildTarget>): Boolean =
      targets.all {
        it.kind.isJvmTarget() && it.kind.ruleType != RuleType.TEST
      }

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}

internal class RunScriptPathCommandLineState(
  environment: ExecutionEnvironment,
  private val settings: JvmRunState,
  configuration: BazelRunConfiguration,
) :
  JvmDebuggableCommandLineState(environment, settings.debugPort, configuration) {
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
  if (environment.runProfile !is BazelRunConfiguration) return@buildList

  if (environment.executor is DefaultDebugExecutor) {
    // https://bazel.build/reference/command-line-reference#flag--java_debug
    // https://github.com/bazelbuild/rules_java/blob/747bddd6091a624c54a42c1ac20308190c1ad849/java/bazel/rules/java_stub_template.txt#L23
    this += "--wrapper_script_flag=--debug=$debugPort"
  }
  this += getJvmDebuggerAgentVmOptions(environment).map { wrapVmOptionAsArg(it) }
  this += getJvmRunExtensionVmOptions(environment).map { wrapVmOptionAsArg(it) }
}

/**
 * The VM options of the coroutine agent and the debugger agent when [environment] debugs, else an empty list.
 * The JDWP agent option is not in the list.
 */
@ApiStatus.Internal
fun getJvmDebuggerAgentVmOptions(environment: ExecutionEnvironment): List<String> {
  if (environment.executor !is DefaultDebugExecutor) return emptyList()
  val debugParameters = JavaParameters()
  debugParameters.vmParametersList.addAll(retrieveKotlinCoroutineParams(environment, environment.project))
  // async stacks and the log-capture console decoration require the debugger agent inside the debuggee
  addDebuggerAgent(debugParameters, environment.project, false)
  return debugParameters.vmParametersList.parameters
}

/** The VM options that the run configuration extensions add, for example for the profiler or the coverage. */
@ApiStatus.Internal
fun getJvmRunExtensionVmOptions(environment: ExecutionEnvironment): List<String> {
  val configuration = environment.runProfile as? BazelRunConfiguration ?: return emptyList()
  val profilerParameters = JavaParameters()
  JavaRunConfigurationExtensionManager.instance.updateJavaParameters(
    configuration,
    profilerParameters,
    environment.runnerSettings,
    environment.executor,
  )
  return profilerParameters.vmParametersList.parameters
}

private fun wrapVmOptionAsArg(vmOption: String): String {
  // https://github.com/bazelbuild/rules_java/blob/747bddd6091a624c54a42c1ac20308190c1ad849/java/bazel/rules/java_stub_template.txt#L33
  return "--wrapper_script_flag=--jvm_flag=$vmOption"
}

@ApiStatus.Internal
fun attachJvmRunExtensions(environment: ExecutionEnvironment, processHandler: OSProcessHandler) {
  val configuration = environment.runProfile as? BazelRunConfiguration ?: return
  JavaRunConfigurationExtensionManager.instance.attachExtensionsToProcess(configuration, processHandler, environment.runnerSettings)
}
