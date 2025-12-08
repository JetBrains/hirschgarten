package org.jetbrains.bazel.golang.debug

import com.goide.execution.application.GoApplicationConfiguration
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bazel.sync.projectStructure.legacy.WorkspaceModuleUtils
import org.jetbrains.bsp.protocol.BuildTarget
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/** Used to store a runner to an [ExecutionEnvironment].  */
internal val EXECUTABLE_KEY: Key<AtomicReference<ExecutableInfo>> = Key.create("bazel.debug.golang.executable")

class BazelGoTestHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    configuration.setBeforeRunTasksFromHandler(
      listOfNotNull(
        BazelGoTestBeforeRunTaskProvider().createTask(configuration),
      ),
    )
  }

  override val name: String
    get() = "Bazel Go Test Handler"

  override val state = GenericTestState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        environment.putCopyableUserData(EXECUTABLE_KEY, AtomicReference())
        val target = getTargetId(environment)
        val project = environment.project
        val module = WorkspaceModuleUtils.findModule(project) ?: error("Could not find module for target $target")
        GoTestWithDebugCommandLineState(
          environment = environment,
          module = module,
          configuration = GoApplicationConfiguration(project, "default", BazelRunConfigurationType()),
          settings = state,
        )
      }

      else -> {
        BazelTestCommandLineState(environment, state)
      }
    }

  private fun getTargetId(environment: ExecutionEnvironment): Label =
    (environment.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()
      ?: throw ExecutionException(BazelPluginBundle.message("go.test.handler.error.target.missing.from.config"))

  class BazelGoTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "BazelGoTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelGoTestHandler = BazelGoTestHandler(configuration)

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      BazelFeatureFlags.isGoSupportEnabled && targetInfos.all { it.kind.includesGo() && it.kind.ruleType == RuleType.TEST }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazeGoTestConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}

open class GoTestWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  module: Module,
  configuration: GoApplicationConfiguration,
  val settings: GenericTestState,
) : GoDebuggableCommandLineState(environment, module, configuration) {
  override fun patchAdditionalConfigs() {
    with(configuration) {
      val testFilter = settings.testFilter
      if (testFilter != null) {
        customEnvironment["TESTBRIDGE_TEST_ONLY"] = testFilter
      }
      val envVarsData = settings.env
      val envVars = envVarsData.envs
      for (env in envVars) {
        customEnvironment[env.key] = env.value
      }
      isPassParentEnvironment = envVarsData.isPassParentEnvs
    }
  }
}

data class ExecutableInfo(
  val binary: File,
  val workingDir: File,
  val args: List<String?>,
  val envVars: Map<String, String>,
)
