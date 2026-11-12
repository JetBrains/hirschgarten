package org.jetbrains.bazel.golang.debug

import com.goide.execution.application.GoApplicationConfiguration
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.golang.resolve.BazelGoBundle
import org.jetbrains.bazel.golang.targetKinds.includesGo
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericRunState
import java.util.concurrent.atomic.AtomicReference
import org.jetbrains.bsp.protocol.BuildTargetInfo

internal class BazelGoRunHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    configuration.setBeforeRunTasksFromHandler(
      listOfNotNull(
        ScriptPathDebugBeforeRunTaskProvider().createTask(configuration),
      ),
    )
  }

  override val name: @Nls String
    get() = BazelGoBundle.message("go.run.handler.name")

  override val isTestHandler: Boolean = false

  override val state = GenericRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        environment.putCopyableUserData(EXECUTABLE_KEY, AtomicReference())
        val target = getTargetId(environment)
        val module = GoWorkspaceModuleUtil.findModule(environment.project) ?: error("Could not find module for target $target")
        val configuration = GoApplicationConfiguration(environment.project, "default", BazelRunConfigurationType())
        configuration.setModule(module)
        GoRunWithDebugCommandLineState(
          environment = environment,
          module = module,
          configuration = configuration,
          settings = state,
        )
      }

      else -> {
        BazelRunCommandLineState(environment, state)
      }
    }

  private fun getTargetId(environment: ExecutionEnvironment): Label =
    (environment.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()
      ?: throw ExecutionException(BazelPluginBundle.message("go.runner.wrong.configuration"))

  class BazelGoRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "BazelGoRunHandlerProvider"

    override val providedRunHandlerName: @Nls String
      get() = BazelGoBundle.message("go.run.handler.name")

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = BazelGoRunHandler(configuration)

    override fun canRun(project: Project, targets: List<BuildTargetInfo>): Boolean =
      targets.all { it.kind.includesGo() && it.kind.ruleType == RuleType.BINARY }

    override val googleHandlerId: String = "BlazeGoRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}
