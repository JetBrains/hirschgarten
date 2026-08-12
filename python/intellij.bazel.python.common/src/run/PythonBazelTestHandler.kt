package org.jetbrains.bazel.python.run

import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.python.lang.PythonLanguageClass
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.run.state.GenericTestState
import org.jetbrains.bsp.protocol.TestParams

@ApiStatus.Internal
class PythonBazelTestHandler : PythonBazelHandler<GenericTestState>() {
  override val name: String
    get() = "Python Test Handler"

  override val isTestHandler: Boolean = true

  override val state: GenericTestState = GenericTestState()

  override fun createCommandLineState(environment: ExecutionEnvironment): BazelCommandLineStateBase =
    BazelTestCommandLineState(environment, state, ::addJunitXmlOptionsToEnvironment)

  @VisibleForTesting
  fun addJunitXmlOptionsToEnvironment(testParams: TestParams): TestParams {
    val envs = testParams.environmentVariables.orEmpty().toMutableMap()
    val originalPytestOpts = envs["PYTEST_ADDOPTS"]
    if (originalPytestOpts == null) {
      envs["PYTEST_ADDOPTS"] = $$"--junitxml=${XML_OUTPUT_FILE} -o junit_family=xunit1"
    } else if (!originalPytestOpts.contains("junitxml")) {
      envs["PYTEST_ADDOPTS"] = $$"$$originalPytestOpts --junitxml=${XML_OUTPUT_FILE} -o junit_family=xunit1"
    } else {
      // if the user has already specified --junitxml, we don't want to interfere with that
      return testParams
    }

    return testParams.copy(
      environmentVariables = envs
    )
  }

  class Provider : GooglePluginAwareRunHandlerProvider {
    override val id: String
      get() = "PythonBazelTestHandlerProvider"

    override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = PythonBazelTestHandler()

    override fun canRun(targets: List<TargetKind>): Boolean =
      targets.all {
        it.languageClasses.contains(PythonLanguageClass.PYTHON) && it.ruleType == RuleType.TEST
      }

    override val googleHandlerId: String = "BlazePyTestConfigurationHandlerProvider"
    override val isTestHandler: Boolean = true
  }
}
