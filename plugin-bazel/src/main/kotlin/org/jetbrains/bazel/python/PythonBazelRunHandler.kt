package org.jetbrains.bazel.python

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bsp.protocol.BuildTarget
import java.util.UUID

class PythonBazelRunHandler : BazelRunHandler {
  override val name: String = "Python Run Handler"

  override val state = GenericRunState()

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    return if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
      PythonDebugCommandLineState(environment, originId, state)
    } else {
      BazelRunCommandLineState(environment, originId, state)
    }
  }

  class Provider : GooglePluginAwareRunHandlerProvider {
    override val id: String = "PythonBazelRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = PythonBazelRunHandler()

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      targetInfos.all {
        it.languageIds.contains("python") && it.kind.ruleType == RuleType.BINARY
      }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazePyRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}
