package org.jetbrains.plugins.bsp.runConfig

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner

public class BspRunRunner : ProgramRunner<RunnerSettings> {
  override fun getRunnerId(): String {
    return ID
  }

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    val isRunExecutor = executorId == DefaultRunExecutor.EXECUTOR_ID // We're only interested in Run, not Debug/Profile
    return isRunExecutor && profile is BspRunConfiguration
  }

  override fun execute(environment: ExecutionEnvironment) {
    val state = environment.state ?: return
    state.execute(environment.executor, this)
  }

  public companion object {
    public const val ID: String = "BspRunRunner"
  }
}
