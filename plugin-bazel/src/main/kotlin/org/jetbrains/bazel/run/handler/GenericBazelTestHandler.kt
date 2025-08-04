package org.jetbrains.bazel.run.handler

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.state.GenericTestState

class GenericBazelTestHandler : BazelRunHandler {
  override val state: GenericTestState = GenericTestState()

  override val name: String
    get() = "Generic BSP Test Handler"

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    BazelTestCommandLineState(environment, state)
}
