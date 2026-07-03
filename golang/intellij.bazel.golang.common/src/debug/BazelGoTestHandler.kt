package org.jetbrains.bazel.golang.debug

import com.goide.execution.testing.GoTestRunConfiguration
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.golang.targetKinds.includesGo
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericTestState
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Used to store a runner to an [ExecutionEnvironment].  */
internal val EXECUTABLE_KEY: Key<AtomicReference<ExecutableInfo>> = Key.create("bazel.debug.golang.executable")

// ensures proper test output recognized by the Go test runner
// without this env the test results are not shown properly in the IDE
private val GO_TEST_WRAP_TESTV_1 = mapOf("GO_TEST_WRAP_TESTV" to "1")

internal class BazelGoTestHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    configuration.setBeforeRunTasksFromHandler(
      listOfNotNull(
        ScriptPathDebugBeforeRunTaskProvider().createTask(configuration),
      ),
    )
  }

  override val name: String
    get() = "Bazel Go Test Handler"

  override val isTestHandler: Boolean = true

  override val state = GenericTestState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        environment.putCopyableUserData(EXECUTABLE_KEY, AtomicReference())
        val target = getTargetId(environment)
        val project = environment.project
        val module = GoWorkspaceModuleUtil.findModule(project) ?: error("Could not find module for target $target")
        val configuration = GoTestRunConfiguration(project, "default", BazelRunConfigurationType())
        configuration.setModule(module)
        GoTestWithDebugCommandLineState(
          environment = environment,
          module = module,
          configuration = configuration,
          settings = state,
        )
      }
      else -> BazelTestCommandLineState(environment, state) {
        it.copy(environmentVariables = GO_TEST_WRAP_TESTV_1 + it.environmentVariables.orEmpty())
      }
    }

  private fun getTargetId(environment: ExecutionEnvironment): Label =
    (environment.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()
      ?: throw ExecutionException(BazelPluginBundle.message("go.test.handler.error.target.missing.from.config"))

  class BazelGoTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "BazelGoTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelGoTestHandler = BazelGoTestHandler(configuration)

    override fun canRun(targets: List<TargetKind>): Boolean =
      BazelFeatureFlags.isGoSupportEnabled && targets.all { it.includesGo() && it.ruleType == RuleType.TEST }

    override val googleHandlerId: String = "BlazeGoTestConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}

internal data class ExecutableInfo(
  val binary: File,
  val workingDir: File,
  val args: List<String?>,
  val envVars: Map<String, String>,
)
