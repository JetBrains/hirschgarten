package com.intellij.bazel.java.profiler

import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.profiler.api.configurations.ProfilerConfigurationState
import com.intellij.profiler.ultimate.NewProcessStartedExternallyCommunicator
import com.intellij.profiler.ultimate.jfr.JFRConfigurationExtensionBase
import com.intellij.profiler.ultimate.jfr.JFRProfilerProcess
import com.intellij.profiler.ultimate.jfr.RecordingConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfiguration

internal class BazelJFRConfigurationExtension : JFRConfigurationExtensionBase() {
  override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean =
    super.isApplicableFor(configuration) && configuration is BazelRunConfiguration

  override fun attachToProcess(
    profilerConfigurationState: ProfilerConfigurationState,
    profilerParameters: RecordingConfiguration,
    runConfiguration: RunConfigurationBase<*>,
    handler: ProcessHandler,
  ) {
    if (handler !is OSProcessHandler) return

    val communicator = NewProcessStartedExternallyCommunicator(runConfiguration.name, handler)
    val process = JFRProfilerProcess(
      runConfiguration.project,
      communicator,
      profilerConfigurationState,
      profilerParameters,
      System.currentTimeMillis(),
    )
    openToolWindowAndReportStart(process, runConfiguration)
  }
}
