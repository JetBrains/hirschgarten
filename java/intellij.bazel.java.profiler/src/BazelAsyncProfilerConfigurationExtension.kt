package com.intellij.bazel.java.profiler

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.profiler.api.configurations.ProfilerConfigurationState
import com.intellij.profiler.ultimate.NewProcessStartedExternallyCommunicator
import com.intellij.profiler.ultimate.async.AgentConfiguration
import com.intellij.profiler.ultimate.async.AsyncProfilerConfigurationExtensionBase
import com.intellij.profiler.ultimate.async.AsyncProfilerProcess
import org.jetbrains.bazel.run.config.BazelRunConfiguration

internal class BazelAsyncProfilerConfigurationExtension : AsyncProfilerConfigurationExtensionBase() {
  override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean =
    super.isApplicableFor(configuration) && configuration is BazelRunConfiguration

  override fun attachToProcess(
    profilerConfigurationState: ProfilerConfigurationState,
    profilerParameters: AgentConfiguration,
    runConfiguration: RunConfigurationBase<*>,
    handler: ProcessHandler,
  ) {
    if (handler !is OSProcessHandler) return

    val communicator = NewProcessStartedExternallyCommunicator(runConfiguration.name, handler)
    val process = AsyncProfilerProcess(
      runConfiguration.project,
      communicator,
      profilerConfigurationState,
      profilerParameters,
      System.currentTimeMillis(),
    )
    openToolWindowAndReportStart(process, runConfiguration)
  }
}
