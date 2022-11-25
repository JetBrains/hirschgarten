package org.jetbrains.plugins.bsp.run

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.runners.ExecutionEnvironment

public abstract class BspRunStateBase(
  environment: ExecutionEnvironment,
  private val runConfiguration: BspRunConfiguration,
  private val config: BspRunConfiguration,
): CommandLineState(environment) {

}