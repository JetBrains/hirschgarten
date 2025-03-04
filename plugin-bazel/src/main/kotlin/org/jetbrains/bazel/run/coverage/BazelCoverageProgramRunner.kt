package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageExecutor
import com.intellij.coverage.CoverageHelper
import com.intellij.coverage.CoverageRunnerData
import com.intellij.execution.configurations.ConfigurationInfoProvider
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.ExecutionUiService
import com.intellij.execution.ui.RunContentDescriptor
import org.jetbrains.bazel.run.commandLine.BazelTestCommandLineState
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories

private const val RUNNER_ID = "BazelCoverageProgramRunner"

class BazelCoverageProgramRunner : GenericProgramRunner<RunnerSettings>() {
  override fun getRunnerId(): String = RUNNER_ID

  override fun canRun(executorId: String, profile: RunProfile): Boolean {
    if (executorId != CoverageExecutor.EXECUTOR_ID) return false
    if (profile !is BazelRunConfiguration) return false
    return BazelCoverageEngine.getInstance().isApplicableTo(profile)
  }

  override fun createConfigurationData(settingsProvider: ConfigurationInfoProvider): RunnerSettings? = CoverageRunnerData()

  override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
    val configuration = environment.runProfile as? BazelRunConfiguration ?: return null
    val coverageEnabledConfiguration =
      CoverageEnabledConfiguration.getOrCreate(configuration) as? BazelCoverageEnabledConfiguration ?: return null
    val coverageOutputDirectory = coverageEnabledConfiguration.coverageFilePath?.let { Path.of(it) } ?: return null

    if (state !is BazelTestCommandLineState) return null
    var coverageFileIndex = 0
    state.coverageReportListener = { coverageReport ->
      // There can be several coverage files, e.g. if we run several test targets at once. Save them to a directory.
      val destinationPath = coverageOutputDirectory.resolve("${coverageFileIndex++}")
      destinationPath.createParentDirectories()
      coverageReport.copyTo(destinationPath)
    }

    val executionResult = state.execute(environment.executor, this)
    CoverageHelper.attachToProcess(configuration, executionResult.processHandler, environment.runnerSettings)

    return ExecutionUiService.getInstance().showRunContent(executionResult, environment)
  }
}
