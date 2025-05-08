package org.jetbrains.bazel.python.run

import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bsp.protocol.BuildTarget

class PythonBazelRunHandler : PythonBazelHandler() {
  override val name: String = "Python Run Handler"

  override val state = GenericRunState()

  override fun createCommandLineState(environment: ExecutionEnvironment, originId: String): BazelCommandLineStateBase =
    BazelRunCommandLineState(environment, originId, state)

  override fun getProgramArguments(): String? = state.programArguments

  class Provider : GooglePluginAwareRunHandlerProvider {
    override val id: String = "PythonBazelRunHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = PythonBazelRunHandler()

    override fun canRun(targetInfos: List<BuildTarget>): Boolean =
      targetInfos.all {
        it.kind.languageClasses.contains(LanguageClass.PYTHON) && it.kind.ruleType == RuleType.BINARY
      }

    override fun canDebug(targetInfos: List<BuildTarget>): Boolean = canRun(targetInfos)

    override val googleHandlerId: String = "BlazePyRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}
