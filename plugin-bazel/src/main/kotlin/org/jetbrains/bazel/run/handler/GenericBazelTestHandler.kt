package org.jetbrains.bazel.run.handler

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.state.GenericTestState
import java.util.UUID

class GenericBazelTestHandler : BazelRunHandler {
  override val state: GenericTestState = GenericTestState()

  override val name: String = "Generic BSP Test Handler"

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    return BazelTestCommandLineState(environment, originId, state)
  }
}
