package org.jetbrains.bazel.run.test

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.execution.testframework.ui.BaseTestsOutputConsoleView
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.AbstractGenericTestState

internal class BazelRerunFailedTestsAction(
  consoleView: BaseTestsOutputConsoleView,
) : AbstractRerunFailedTestsAction(consoleView.console) {
  init {
    init(consoleView.properties)
  }

  override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile? {
    val configuration = (myConsoleProperties.configuration as? BazelRunConfiguration)?.clone() as? BazelRunConfiguration ?: return null
    val handler = configuration.handler ?: return null
    val state = handler.state as? AbstractGenericTestState<*> ?: return null

    val failedTests = getFailedTests(configuration.project)
    if (configuration.project.useJetBrainsTestRunner()) {
      val failedTestIds = failedTests.getTestIds()
      if (failedTestIds.isEmpty()) return null
      setTestUniqueIds(state = state, testUniqueIds = failedTestIds)
    } else {
      // Any other runner: re-run the failed tests via a generic bazel --test_filter.
      val testFilter = failedTestsToFilter(failedTests) ?: return null
      setTestFilter(configuration.project, state, testFilter)
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

/**
 * Builds a bazel `--test_filter` (an alternation regex) selecting exactly the failed [failedTests],
 * for runners other than the JetBrains one, using the same per-language formatting as the gutter
 * and results-tree context menu (see [BazelTestFilterProvider]).
 *
 * Only leaf tests are used: [getFailedTests] also returns the defective parent classes/containers,
 * and turning one of those into a filter (e.g. a bare `FooTest`) would re-run the whole class --
 * every test in it -- not just the failures. Returns null if nothing matched.
 */
internal fun failedTestsToFilter(failedTests: List<AbstractTestProxy>): String? =
  failedTests
    .filter { it.isLeaf }
    .mapNotNull { it.locationUrl?.let(BazelTestFilterProvider::testFilterFor) }
    .distinct()
    .joinToString("|")
    .ifEmpty { null }
