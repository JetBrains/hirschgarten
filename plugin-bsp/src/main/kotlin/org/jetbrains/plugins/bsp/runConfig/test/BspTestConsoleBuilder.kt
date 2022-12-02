package org.jetbrains.plugins.bsp.runConfig.test

import com.intellij.execution.Executor
import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.psi.search.ExecutionSearchScopes
import org.jetbrains.plugins.bsp.runConfig.BspRunConfiguration

public class BspTestConsoleBuilder(
  project: Project,
  private val config: BspRunConfiguration,
  private val executor: Executor,
) : TextConsoleBuilderImpl(project, ExecutionSearchScopes.executionScope(project, config)) {
  override fun createConsole(): ConsoleView {
    val consoleProperties = BspTestConsoleProperties(config, executor)
    return SMTestRunnerConnectionUtil.createConsole("BSP", consoleProperties)
  }
}
