package org.jetbrains.bazel.golang.debug

import com.goide.execution.application.GoApplicationConfiguration
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.includesGo
import org.jetbrains.bazel.sync.projectStructure.legacy.WorkspaceModuleUtils
import org.jetbrains.bazel.taskEvents.OriginId
import org.jetbrains.bsp.protocol.BuildTarget
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class BazelGoRunHandler(configuration: BazelRunConfiguration) : BazelRunHandler {
  init {
    configuration.beforeRunTasks =
      listOfNotNull(
        BazelGoCalculateExecutableInfoBeforeRunTaskProvider().createTask(configuration),
      )
  }

  override val name: String = "Bazel Go Run Handler"

  override val state = GenericRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    when {
      executor is DefaultDebugExecutor -> {
        environment.putCopyableUserData(EXECUTABLE_KEY, AtomicReference())
        val config = GoApplicationConfiguration(environment.project, "default", BazelRunConfigurationType())
        val target = getTargetId(environment)
        val module = WorkspaceModuleUtils.findModule(environment.project) ?: error("Could not find module for target $target")
        GoRunWithDebugCommandLineState(environment, UUID.randomUUID().toString(), module, config, state)
      }

      else -> {
        BazelRunCommandLineState(environment, UUID.randomUUID().toString(), state)
      }
    }

  private fun getTargetId(environment: ExecutionEnvironment): Label =
    (environment.runProfile as? BazelRunConfiguration)?.targets?.singleOrNull()
      ?: throw ExecutionException(BazelPluginBundle.message("go.runner.wrong.configuration"))

  class BazelGoRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
    override val id: String = "BazelGoRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = BazelGoRunHandler(configuration)

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      BazelFeatureFlags.isGoSupportEnabled && targetInfos.all { it.kind.includesGo() && it.kind.ruleType == RuleType.BINARY }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazeGoRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}

class GoRunWithDebugCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  module: Module,
  configuration: GoApplicationConfiguration,
  val settings: GenericRunState,
) : GoDebuggableCommandLineState(environment, module, configuration, originId)
