package org.jetbrains.bazel.python.run

import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericTestState

internal class PythonBazelTestHandler : PythonBazelHandler<GenericTestState>() {
  override val name: String
    get() = "Python Test Handler"

  override val isTestHandler: Boolean = true

  override val state: GenericTestState = GenericTestState()

  override fun createCommandLineState(environment: ExecutionEnvironment): BazelCommandLineStateBase =
    BazelTestCommandLineState(environment, state)

  class Provider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "PythonBazelTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = PythonBazelTestHandler()

    override fun canRun(targets: List<TargetKind>): Boolean =
      targets.all {
        it.languageClasses.contains(LanguageClass.PYTHON) && it.ruleType == RuleType.TEST
      }

    override fun canDebug(targets: List<TargetKind>): Boolean = canRun(targets)

    override val googleHandlerId: String = "BlazePyTestConfigurationHandlerProvider"
    override val isTestHandler: Boolean = true
  }
}
