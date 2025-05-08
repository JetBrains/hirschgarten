package org.jetbrains.bazel.python.run

import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bsp.protocol.BuildTarget

class PythonBazelTestHandler : PythonBazelHandler() {
  override val name: String = "Python Test Handler"

  override val state = GenericTestState()

  override fun createCommandLineState(environment: ExecutionEnvironment, originId: String): BazelCommandLineStateBase =
    BazelTestCommandLineState(environment, originId, state)

  override fun getProgramArguments(): String? = state.programArguments

  class Provider : GooglePluginAwareRunHandlerProvider {
    override val id: String = "PythonBazelTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = PythonBazelTestHandler()

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      targetInfos.all {
        it.kind.languageClasses.contains(LanguageClass.PYTHON) && it.kind.ruleType == RuleType.TEST
      }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazePyTestConfigurationHandlerProvider"
    override val isTestHandler: Boolean = true
  }
}
