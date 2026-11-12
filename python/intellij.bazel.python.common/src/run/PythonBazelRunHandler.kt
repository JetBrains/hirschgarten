package org.jetbrains.bazel.python.run

import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bsp.protocol.BuildTargetInfo

internal class PythonBazelRunHandler : PythonBazelHandler<GenericRunState>() {
  override val name: @Nls String
    get() = BazelPluginBundle.message("python.run.handler.name")

  override val isTestHandler: Boolean = false

  override val state: GenericRunState = GenericRunState()

  override fun createCommandLineState(environment: ExecutionEnvironment): BazelCommandLineStateBase =
    BazelRunCommandLineState(environment, state)

  class Provider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "PythonBazelRunHandlerProvider"

    override val providedRunHandlerName: @Nls String
      get() = BazelPluginBundle.message("python.run.handler.name")

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = PythonBazelRunHandler()

    override fun canRun(project: Project, targets: List<BuildTargetInfo>): Boolean =
      targets.all {
        it.kind.languageClasses.contains(PythonLanguageClass.PYTHON) && it.kind.ruleType == RuleType.BINARY
      }

    override val googleHandlerId: String = "BlazePyRunConfigurationHandlerProvider"
    override val isTestHandler: Boolean = false
  }
}
