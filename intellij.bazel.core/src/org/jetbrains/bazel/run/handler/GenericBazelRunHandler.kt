package org.jetbrains.bazel.run.handler

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.annotations.Nls
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.commandLine.BazelRunCommandLineState
import org.jetbrains.bazel.run.state.GenericRunState

internal class GenericBazelRunHandler : BazelRunHandler {
  override val state: GenericRunState = GenericRunState()

  override val name: @Nls String
    get() = BazelPluginBundle.message("generic.run.handler.name")

  override val isTestHandler: Boolean = false

  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    BazelRunCommandLineState(environment, state)
}
