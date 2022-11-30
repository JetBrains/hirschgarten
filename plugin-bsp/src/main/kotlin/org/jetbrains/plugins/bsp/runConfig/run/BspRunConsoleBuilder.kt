package org.jetbrains.plugins.bsp.runConfig.run

import com.intellij.execution.filters.TextConsoleBuilderImpl
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.Project
import com.intellij.psi.search.ExecutionSearchScopes
import org.jetbrains.plugins.bsp.runConfig.BspConsoleView
import org.jetbrains.plugins.bsp.runConfig.BspRunConfiguration

public class BspRunConsoleBuilder(
  project: Project,
  private val config: BspRunConfiguration,
) : TextConsoleBuilderImpl(project, ExecutionSearchScopes.executionScope(project, config)) {

  override fun createConsole(): ConsoleView =
    BspConsoleView(project, scope, isViewer, true)
}
