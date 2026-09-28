package org.jetbrains.bazel.clion.run

import com.intellij.cidr.debugger.profiles.CidrDebugProfilesEnabler
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.clion.BazelCLionCoreBundle
import org.jetbrains.bazel.clion.BazelCLionFeatureFlags
import org.jetbrains.bazel.clion.sync.CC_LANGUAGE_CLASS
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run.BazelRunConfigurationState
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.GenericRunState
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

  class Provider : RunHandlerProvider {

    override val id: String = "CcRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = CcRunHandler()

    override fun canRun(project: Project, targets: List<BuildTarget>): Boolean {
      if (!BazelCLionFeatureFlags.isCLionEnabled) return false

      val target = targets.singleOrNull() ?: return false
      return CC_LANGUAGE_CLASS in target.kind.languageClasses && target.kind.ruleType == RuleType.BINARY
    }
  }

  class DebugProfileEnabler : CidrDebugProfilesEnabler {

    override fun debugProfilesEnabled(project: Project, runConfiguration: RunConfiguration?): Boolean {
      return runConfiguration is BazelRunConfiguration && runConfiguration.handler is CcRunHandler
    }
  }
}
