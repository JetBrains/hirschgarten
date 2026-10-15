package org.jetbrains.bazel.clion.run

import com.intellij.cidr.debugger.profiles.CidrDebugProfilesEnabler
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.clion.BazelCLionFeatureFlags
import org.jetbrains.bazel.clion.sync.includesCc
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bsp.protocol.BuildTarget

internal class CcRunHandler : BazelRunHandler {
  override val state: GenericRunState = GenericRunState()

  override val name: String get() = BazelCLionCoreBundle.message("run.handler.cc.name")

  override val isTestHandler: Boolean = false

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return if (executor is DefaultDebugExecutor) {
      CcDebugCommandLineState(environment, state)
    } else {
      BazelRunCommandLineState(environment, state)
    }
  }

  internal class Provider : RunHandlerProvider {
    override val id: String = "CcRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = CcRunHandler()

    override fun canRun(project: Project, targets: List<BuildTarget>): Boolean {
      if (!BazelCLionFeatureFlags.isCLionEnabled) return false

      val target = targets.singleOrNull() ?: return false
      return target.kind.includesCc() && target.kind.ruleType == RuleType.BINARY
    }
  }
}

@ApiStatus.Internal
class CcTestHandler : BazelRunHandler {
  override val state: GenericTestState = GenericTestState()

  override val name: String get() = BazelCLionCoreBundle.message("test.handler.cc.name")

  override val isTestHandler: Boolean = true

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    return if (executor is DefaultDebugExecutor) {
      CcDebugCommandLineState(environment, state)
    }
    else {
      BazelTestCommandLineState(environment, state)
    }
  }

  internal class Provider : RunHandlerProvider {
    override val id: String = "CcTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = CcTestHandler()

    override fun canRun(project: Project, targets: List<BuildTarget>): Boolean {
      if (!BazelCLionFeatureFlags.isCLionEnabled) return false
      return targets.all { it.kind.includesCc() && it.kind.ruleType == RuleType.TEST }
    }
  }
}

internal class DebugProfileEnabler : CidrDebugProfilesEnabler {
  override fun debugProfilesEnabled(project: Project, runConfiguration: RunConfiguration?): Boolean {
    return runConfiguration is BazelRunConfiguration &&
           (runConfiguration.handler is CcRunHandler || runConfiguration.handler is CcTestHandler)
  }
}
