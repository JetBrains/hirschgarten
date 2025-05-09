package org.jetbrains.bazel.python.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.python.debug.PythonDebugCommandLineState
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.BazelRunHandler
import java.util.UUID

abstract class PythonBazelHandler : BazelRunHandler {
  override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
    val originId = UUID.randomUUID().toString()
    return if (executor.id == DefaultDebugExecutor.EXECUTOR_ID) {
      PythonDebugCommandLineState(environment, originId, getProgramArguments())
    } else {
      createCommandLineState(environment, originId)
    }
  }

  protected abstract fun createCommandLineState(environment: ExecutionEnvironment, originId: String): BazelCommandLineStateBase

  protected abstract fun getProgramArguments(): String?
}
