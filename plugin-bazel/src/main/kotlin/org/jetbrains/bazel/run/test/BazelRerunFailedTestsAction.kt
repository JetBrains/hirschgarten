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

class BazelRerunFailedTestsAction(
  consoleView: BaseTestsOutputConsoleView,
) : AbstractRerunFailedTestsAction(consoleView.console) {
  init {
    init(consoleView.properties)
  }

  override fun getRunProfile(environment: ExecutionEnvironment): MyRunProfile? {
    val configuration = (myConsoleProperties.configuration as? BazelRunConfiguration)?.clone() as? BazelRunConfiguration ?: return null
    val failedTestIds = getFailedTests(configuration.project).getTestIds()
    if (failedTestIds.isEmpty()) return null
    val handler = configuration.handler ?: return null

    val state = handler.state as? AbstractGenericTestState<*> ?: return null
    setTestUniqueIds(state = state, testUniqueIds = failedTestIds)
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

fun List<AbstractTestProxy>.getTestIds(): List<String> =
  filter { it.metainfo == "test" }
  .mapNotNull { it.getUserData(SMTestProxy.NODE_ID) }
