package org.jetbrains.plugins.bsp.runConfig.test

import com.intellij.execution.Executor
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import org.jetbrains.plugins.bsp.runConfig.BspRunConfiguration

public class BspTestConsoleProperties(
  config: BspRunConfiguration,
  executor: Executor,
) : SMTRunnerConsoleProperties(config, "BSP Test", executor) {

  init {
    isIdBasedTestTree = true
  }

  // TODO: implement
  override fun getTestLocator(): SMTestLocator? = null
}