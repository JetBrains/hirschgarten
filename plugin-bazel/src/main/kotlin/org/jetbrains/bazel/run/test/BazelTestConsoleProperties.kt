package org.jetbrains.bazel.run.test

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bazel.config.BazelPluginConstants

class BazelTestConsoleProperties(
  config: RunConfiguration,
  executor: Executor,
) : SMTRunnerConsoleProperties(config, BazelPluginConstants.BAZEL_DISPLAY_NAME, executor) {
  override fun getTestLocator(): SMTestLocator? = BazelTestLocatorProvider.ep.extensionList.firstOrNull()?.getTestLocator()
}

interface BazelTestLocatorProvider {
  fun getTestLocator(): SMTestLocator

  companion object {
    val ep = ExtensionPointName.create<BazelTestLocatorProvider>("org.jetbrains.bazel.testLocatorProvider")
  }
}
