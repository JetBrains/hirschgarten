package org.jetbrains.bazel.run.handler

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BspRunHandler
import org.jetbrains.bazel.run.commandLine.BspTestCommandLineState
import org.jetbrains.bazel.run.state.GenericTestState
import java.util.UUID

class GenericBspTestHandler : BspRunHandler {
  override val state: GenericTestState = GenericTestState()

  override val name: String = "Generic BSP Test Handler"

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    return BspTestCommandLineState(environment, originId, state)
  }
}
