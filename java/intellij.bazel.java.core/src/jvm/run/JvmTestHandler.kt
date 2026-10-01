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
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.isJvmTarget
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.TestParams

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
      JvmTestCommandLineState(environment, state)
    }
  }

  override val extensionsManager: RunConfigurationExtensionsManager<in RunConfigurationBase<*>, *>
    get() = JavaRunConfigurationExtensionManager.instance

  class JvmTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "JvmTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = JvmTestHandler(configuration)

    override fun canRun(project: Project, targets: List<BuildTarget>): Boolean =
      targets.all {
        (it.kind.isJvmTarget() && it.kind.ruleType == RuleType.TEST)
      }

    override fun canRunNonImported(project: Project, targets: List<Label>): Boolean =
      // If a custom extension accepts those targets, then it follows it can run them (even though they aren't imported)
      JvmTestRunnerExtension.getInstance(project, targets) !is DefaultJvmTestRunnerExtension

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = true
  }
}

internal class JvmTestCommandLineState(
  environment: ExecutionEnvironment,
  state: JvmTestState,
) : BazelTestCommandLineState(environment = environment, state = state) {

  private val testRunner by lazy { BazelRunConfiguration.get(environment).getJvmTestRunnerExtension() }
  override val isIdBasedTestTree: Boolean get() = testRunner.isIdBasedTestTree
  override val testRunnerEmitsServiceMessages: Boolean get() = testRunner.testRunnerEmitsServiceMessages

  override fun transformTestParams(params: TestParams): TestParams = testRunner.transformTestParams(params)

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener =
    testRunner.createTaskListener(handler, coverageReportListener)

  override fun createTestRestartActions(console: SMTRunnerConsoleView): Array<AnAction> =
    arrayOf(BazelRerunFailedTestsAction(console))
}

internal class ScriptPathTestCommandLineState(
  environment: ExecutionEnvironment,
  val settings: JvmTestState,
  configuration: BazelRunConfiguration,
) : JvmDebuggableCommandLineState(environment, settings.debugPort, configuration) {
  private val testRunner by lazy { BazelRunConfiguration.get(environment).getJvmTestRunnerExtension() }
  override val isIdBasedTestTree: Boolean get() = testRunner.isIdBasedTestTree
  override val testRunnerEmitsServiceMessages: Boolean get() = testRunner.testRunnerEmitsServiceMessages

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener = testRunner.createTaskListener(handler, null)

  override fun createTestRestartActions(console: SMTRunnerConsoleView): Array<AnAction> =
    arrayOf(BazelRerunFailedTestsAction(console))

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override suspend fun startBsp(
      server: BazelServerFacade,
      pidDeferred: CompletableDeferred<Long?>,
      handler: BazelProcessHandler,
  ) {
    val scriptPath = checkNotNull(environment.getCopyableUserData(SCRIPT_PATH_KEY)?.get()) { "Missing --script_path" }
    val (env, testFilter) = testRunner.transformScriptPathEnvAndTestFilter(settings.env.envs, settings.testFilter)
    runWithScriptPath(
      taskGroupId.task("jvm-test"),
      scriptPath = scriptPath,
      project = environment.project,
      pidDeferred = pidDeferred,
      handler = handler,
      env = env,
      additionalScriptParameters = getAdditionalJvmRunParameters(environment, settings.debugPort),
      isTest = true,
      testFilter = testFilter,
    ) { processHandler ->
      attachJvmRunExtensions(environment, processHandler)
    }
  }
}

private fun BazelRunConfiguration.getJvmTestRunnerExtension(): JvmTestRunnerExtension = JvmTestRunnerExtension.getInstance(project, targets)
