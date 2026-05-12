package org.jetbrains.bazel.java.coverage

import com.intellij.coverage.CoverageExecutor
import com.intellij.coverage.CoverageRunnerData
import com.intellij.execution.configurations.ConfigurationInfoProvider
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.impl.DefaultJavaProgramRunner

private const val RUNNER_ID = "BazelJavaCoverageRunner"

/**
 * [com.intellij.coverage.JavaCoverageRunner]
 */
internal class BazelJavaCoverageRunner : DefaultJavaProgramRunner() {
  override fun canRun(executorId: String, profile: RunProfile): Boolean =
    executorId == CoverageExecutor.EXECUTOR_ID && isJavaAgentCoverageApplicableTo(profile)

  override fun createConfigurationData(settingsProvider: ConfigurationInfoProvider): RunnerSettings {
    return CoverageRunnerData()
  }

  override fun getRunnerId(): String = RUNNER_ID
}
