package org.jetbrains.plugins.bsp.run.handler

import ai.grazie.utils.applyIf
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.commandLine.BspTestCommandLineState
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.run.state.GenericTestState
import java.util.UUID

class GenericBspTestHandler(val configuration: BspRunConfiguration) : BspRunHandler {
  override val state: GenericTestState = GenericTestState()

  override val name: String = "Generic BSP Test Handler"

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    val newState =
      state.applyIf(configuration.singleTestFilter != null) {
        testFilter = configuration.singleTestFilter
      }
    return BspTestCommandLineState(environment, originId, newState)
  }
}
