package org.jetbrains.bazel.python.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.python.debug.PythonDebugCommandLineState
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelRunHandler

abstract class PythonBazelHandler : BazelRunHandler {
  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
      PythonDebugCommandLineState(environment, getProgramArguments())
    } else {
      createCommandLineState(environment)
    }

  protected abstract fun createCommandLineState(environment: ExecutionEnvironment): BazelCommandLineStateBase

  protected abstract fun getProgramArguments(): String?
}
