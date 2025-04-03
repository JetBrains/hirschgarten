package org.jetbrains.bazel.android.run

import com.android.tools.idea.execution.common.AndroidConfigurationExecutor
import com.android.tools.idea.execution.common.AndroidConfigurationProgramRunner
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.bazel.run.config.BazelRunConfiguration

class BazelAndroidProgramRunner : AndroidConfigurationProgramRunner() {
  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    if (profile !is BazelRunConfiguration) return false
    if (profile.handler !is AndroidBazelRunHandler) return false
    return executorId in listOf(DefaultRunExecutor.EXECUTOR_ID, DefaultDebugExecutor.EXECUTOR_ID)
  }

  override val supportedConfigurationTypeIds: List<String>
    get() = throw IllegalStateException("Should not be called because canRun is overriden")

  override fun canRunWithMultipleDevices(executorId: String): Boolean = false

  override fun run(
    environment: ExecutionEnvironment,
    executor: AndroidConfigurationExecutor,
    indicator: ProgressIndicator,
  ): RunContentDescriptor =
    when (environment.executor.id) {
      DefaultRunExecutor.EXECUTOR_ID -> executor.run(indicator)
      DefaultDebugExecutor.EXECUTOR_ID -> executor.debug(indicator)
      else -> throw IllegalStateException("Only run and debug are supported")
    }
}
