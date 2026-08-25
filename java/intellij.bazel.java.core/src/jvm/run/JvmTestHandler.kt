package org.jetbrains.bazel.jvm.run

import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configuration.RunConfigurationExtensionsManager
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.task.BazelRunTaskListener
import org.jetbrains.bazel.run.task.BazelTestTaskListener
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.sync.isJvmTarget
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.TestParams
import java.nio.file.Path
import kotlin.io.path.useLines

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

    override fun canRun(targets: List<TargetKind>): Boolean =
      targets.all {
        (it.isJvmTarget() && it.ruleType == RuleType.TEST)
      }

    override fun canRunNonImported(project: Project, targets: List<Label>): Boolean =
      targetsUseJetBrainsTestRunner(project, targets)

    override val googleHandlerId: String = "BlazeJavaRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = true
  }
}

internal class JvmTestCommandLineState(
  environment: ExecutionEnvironment,
  state: JvmTestState,
) : BazelTestCommandLineState(environment = environment, state = state) {

  private val useJetBrainsTestRunner by lazy { BazelRunConfiguration.get(environment).targetsUseJetBrainsTestRunner() }

  override val isIdBasedTestTree: Boolean get() = useJetBrainsTestRunner

  override val testRunnerEmitsServiceMessages: Boolean get() = useJetBrainsTestRunner

  override fun transformTestParams(params: TestParams): TestParams = when {
    useJetBrainsTestRunner -> params.copy(
      environmentVariables = params.environmentVariables.orEmpty() + JetBrainsTestRunner.envs(params.testFilter),
      testFilter = null,
      streamTestOutput = true,
    )
    else -> params
  }

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener =
    if (useJetBrainsTestRunner) JetBrainsTestRunnerTaskListener(handler) else super.createAndAddTaskListener(handler)

  override fun createTestRestartActions(console: SMTRunnerConsoleView): Array<AnAction> =
    if (useJetBrainsTestRunner) jetBrainsTestRunnerRestartActions(console) else super.createTestRestartActions(console)
}

internal class ScriptPathTestCommandLineState(
  environment: ExecutionEnvironment,
  val settings: JvmTestState,
  configuration: BazelRunConfiguration,
) : JvmDebuggableCommandLineState(environment, settings.debugPort, configuration) {
  private val useJetBrainsTestRunner by lazy { BazelRunConfiguration.get(environment).targetsUseJetBrainsTestRunner() }

  override val isIdBasedTestTree: Boolean get() = useJetBrainsTestRunner

  override val testRunnerEmitsServiceMessages: Boolean get() = useJetBrainsTestRunner

  override fun createAndAddTaskListener(handler: BazelProcessHandler): BazelTaskListener =
    if (useJetBrainsTestRunner) JetBrainsTestRunnerTaskListener(handler) else BazelTestTaskListener(handler)

  override fun createTestRestartActions(console: SMTRunnerConsoleView): Array<AnAction> =
    if (useJetBrainsTestRunner) jetBrainsTestRunnerRestartActions(console) else super.createTestRestartActions(console)

  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult = executeWithTestConsole(executor)

  override suspend fun startBsp(
      server: BazelServerFacade,
      pidDeferred: CompletableDeferred<Long?>,
      handler: BazelProcessHandler,
  ) {
    val scriptPath = checkNotNull(environment.getCopyableUserData(SCRIPT_PATH_KEY)?.get()) { "Missing --script_path" }
    val filter = settings.testFilter
    runWithScriptPath(
      taskGroupId.task("jvm-test"),
      scriptPath = scriptPath,
      project = environment.project,
      pidDeferred = pidDeferred,
      handler = handler,
      env = if (useJetBrainsTestRunner) settings.env.envs + JetBrainsTestRunner.envs(filter) else settings.env.envs,
      additionalScriptParameters = getAdditionalJvmRunParameters(environment, settings.debugPort),
      isTest = true,
      testFilter = if (useJetBrainsTestRunner) null else filter,
    ) { processHandler ->
      attachJvmRunExtensions(environment, processHandler)
    }
  }
}

private const val TEAMCITY_PREFIX = "##teamcity[test"
private const val TEST_NAME_TAG = " name='"
private const val JAVA_TEST_SCHEMA = "java:test://"

private class JetBrainsTestRunnerTaskListener(handler: BazelProcessHandler) : BazelRunTaskListener(handler) {
  override fun onCachedTestLog(testLog: Path) {
    testLog.useLines { lines ->
      lines.map { line ->
        markTestNameAsCached(line)
      }.forEach { line ->
        handler.notifyTextAvailable(line + "\n", ProcessOutputType.STDOUT)
      }
    }
  }

  private fun markTestNameAsCached(line: String): String {
    if (!line.startsWith(TEAMCITY_PREFIX)) return line
    if (JAVA_TEST_SCHEMA !in line) return line
    val nameStart = line.indexOf(TEST_NAME_TAG)
    if (nameStart == -1) return line
    val nameEnd = line.indexOf('\'', startIndex = nameStart + TEST_NAME_TAG.length)
    return line.substring(0 until nameEnd) + " (cached)" + line.substring(nameEnd)
  }
}

private fun jetBrainsTestRunnerRestartActions(console: SMTRunnerConsoleView): Array<AnAction> =
  arrayOf(
    BazelRerunFailedTestsAction(console).apply {
      setModelProvider { console.resultsViewer }
    },
  )
