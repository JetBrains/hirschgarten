package org.jetbrains.bazel.run.test

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.run.BazelTestFinder

class BazelTestConsoleProperties(
  config: RunConfiguration,
  executor: Executor,
) : SMTRunnerConsoleProperties(config, BazelPluginConstants.BAZEL_DISPLAY_NAME, executor) {
  override fun getTestLocator(): SMTestLocator = BazelTestFinder.Locator()
}
