package org.jetbrains.plugins.bsp.runConfig

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project

public abstract class BspRunProfileStateBase(
  protected val project: Project,
  environment: ExecutionEnvironment,
  protected val runConfiguration: BspRunConfiguration,
  protected val consoleBuilder: TextConsoleBuilder
): RunProfileState {


}