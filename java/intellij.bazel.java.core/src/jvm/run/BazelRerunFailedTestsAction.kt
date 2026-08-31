package org.jetbrains.bazel.jvm.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericTestState
import org.jetbrains.bazel.run.test.BazelTestFilterProvider

internal class BazelRerunFailedTestsAction(
  consoleView: SMTRunnerConsoleView,
) : AbstractRerunFailedTestsAction(consoleView.console) {
  init {
    init(consoleView.properties)
    setModelProvider { consoleView.resultsViewer }
  }

  override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile? {
    val configuration = (myConsoleProperties.configuration as? BazelRunConfiguration)?.clone() as? BazelRunConfiguration ?: return null
    val handler = configuration.handler ?: return null
    val state = handler.state as? AbstractGenericTestState<*> ?: return null
    val failedTests = getFailedTests(configuration.project)
    if (configuration.targetsUseJetBrainsTestRunner()) {
      val failedTestIds = failedTests.getTestIds()
      if (failedTestIds.isEmpty()) return null
      JetBrainsTestRunner.setTestUniqueIds(state = state, testUniqueIds = failedTestIds)
    }
    else {
      // Any other runner: re-run the failed tests via a generic bazel --test_filter.
      val testFilter = testFilterFromFailedTests(failedTests) ?: return null
      state.testFilter = testFilter
    }
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

internal fun List<AbstractTestProxy>.getTestIds(): List<String> =
  filter { it.metainfo == "test" }
  .mapNotNull { it.getUserData(SMTestProxy.NODE_ID) }

@ApiStatus.Internal
fun testFilterFromFailedTests(failedTests: List<AbstractTestProxy>): String? =
  failedTests
    // Only leaf tests are used: [AbstractRerunFailedTestsAction.getFailedTests] also returns the defective parent classes/containers,
    // and turning one of those into a filter (e.g. a bare `FooTest`) would re-run the whole class, not just the failures.
    .filter { it.isLeaf }
    .mapNotNull { it.locationUrl?.let(BazelTestFilterProvider::testFilterFor) }
    .distinct()
    .joinToString("|")
    .ifEmpty { null }
