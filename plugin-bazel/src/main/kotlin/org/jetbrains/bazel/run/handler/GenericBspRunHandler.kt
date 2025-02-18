package org.jetbrains.bazel.run.handler

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BspRunHandler
import org.jetbrains.bazel.run.commandLine.BspRunCommandLineState
import org.jetbrains.bazel.run.state.GenericRunState
import java.util.UUID

class GenericBspRunHandler : BspRunHandler {
  override val state: GenericRunState = GenericRunState()

  override val name: String = "Generic BSP Run Handler"

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    return BspRunCommandLineState(environment, originId, state)
  }
}
