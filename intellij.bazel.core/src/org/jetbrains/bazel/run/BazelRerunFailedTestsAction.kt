package org.jetbrains.bazel.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.config.BazelRunConfiguration

internal class BazelRerunFailedTestsAction(
  consoleView: SMTRunnerConsoleView,
  private val testRerunExtension: BazelTestRerunExtension,
) : AbstractRerunFailedTestsAction(consoleView.console) {

  init {
    init(consoleView.properties)
    setModelProvider { consoleView.resultsViewer }
  }

  override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile? {
    val configuration = (myConsoleProperties.configuration as? BazelRunConfiguration)?.clone() as? BazelRunConfiguration ?: return null
    val failedTests = getFailedTests(configuration.project)
    if (!testRerunExtension.setTestsToRerun(configuration, failedLeafTests(failedTests))) return null
    return object : MyRunProfile(configuration) {
      override fun getState(
        executor: Executor,
        environment: ExecutionEnvironment,
      ): RunProfileState? {
        // environment.runProfile is AbstractRerunFailedTestsAction$MyRunProfile here, so we have to pass the configuration manually
        environment.putUserData(BazelRunConfiguration.BAZEL_RUN_CONFIGURATION_KEY, configuration)
        return configuration.getState(executor, environment)
      }
    }
  }
}

@ApiStatus.Internal
fun failedLeafTests(failedTests: List<AbstractTestProxy>): List<AbstractTestProxy> = failedTests.filter { it.isLeaf }
