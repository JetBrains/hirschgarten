package org.jetbrains.plugins.bsp.android.run

import com.android.tools.idea.execution.common.AndroidConfigurationExecutor
import com.android.tools.idea.execution.common.AndroidConfigurationProgramRunner
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfigurationType

public class BspAndroidProgramRunner : AndroidConfigurationProgramRunner() {
  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    if (profile !is BspRunConfiguration) return false
    if (profile.runHandler !is AndroidBspRunHandler) return false
    return super.canRun(executorId, profile)
  }

  override val supportedConfigurationTypeIds: List<String>
    get() = listOf(BspRunConfigurationType.ID)

  override fun canRunWithMultipleDevices(executorId: String): Boolean = false

  override fun run(
    environment: ExecutionEnvironment,
    executor: AndroidConfigurationExecutor,
    indicator: ProgressIndicator,
  ): RunContentDescriptor {
    return when (environment.executor.id) {
      DefaultRunExecutor.EXECUTOR_ID -> executor.run(indicator)
      DefaultDebugExecutor.EXECUTOR_ID -> executor.debug(indicator)
      else -> throw ExecutionException("Only run and debug are supported")
    }
  }
}
