package org.jetbrains.bazel.java.coverage

import com.intellij.coverage.CoverageFileProvider
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuite
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.IDEACoverageRunner
import com.intellij.coverage.JavaCoverageEngine
import com.intellij.coverage.JavaCoverageOptionsProvider
import com.intellij.coverage.JavaCoverageSuite
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.jvm.run.JvmTestHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration

/**
 * https://youtrack.jetbrains.com/issue/BAZEL-3130
 * bazel coverage requires a correctly set `--instrumentation_filter`.
 * Using IDEA's native Java coverage (by using `-javaagent`) also gives us class/method/branch coverage,
 * not just per-line coverage.
 *
 * Only enabled if `run_config_run_with_bazel: false` is in the `.projectview`
 * @see isJavaAgentCoverageApplicableTo
 */
internal class BazelJavaAgentCoverageEngine : JavaCoverageEngine() {
  override fun isApplicableTo(configuration: RunConfigurationBase<*>): Boolean {
    return isJavaAgentCoverageApplicableTo(configuration)
  }

  override fun createEmptyCoverageSuite(coverageRunner: CoverageRunner): CoverageSuite =
    BazelJavaCoverageSuite(this)

  override fun createSuite(
    acceptedCovRunner: CoverageRunner?,
    name: String?,
    coverageDataFileProvider: CoverageFileProvider?,
    filters: Array<String>?,
    excludePatterns: Array<String>?,
    lastCoverageTimeStamp: Long,
    coverageByTestEnabled: Boolean,
    branchCoverage: Boolean,
    trackTestFolders: Boolean,
    project: Project?,
  ): JavaCoverageSuite = BazelJavaCoverageSuite(
    name,
    coverageDataFileProvider,
    filters,
    excludePatterns,
    lastCoverageTimeStamp,
    coverageByTestEnabled,
    branchCoverage,
    trackTestFolders,
    acceptedCovRunner,
    this,
    project,
  )

  /**
   * See [BazelJavaCoverageSuite], we don't want the platform to iterate output jars,
   * so [JavaCoverageEngine.recompileProjectAndRerunAction] will complain about it via a notification. Disable it here
   */
  override fun recompileProjectAndRerunAction(module: Module, suite: CoverageSuitesBundle, chooseSuiteAction: Runnable): Boolean =
    false

  // Use IDEACoverageRunner instead of the default JaCoco, because JaCoCoReportLoader depends on collectOutputRoots.
  // However, we don't want the platform to iterate output jars, see BazelJavaCoverageSuite for more explanation.
  private fun getCoverageRunner() = CoverageRunner.getInstance(IDEACoverageRunner::class.java)

  override fun createCoverageEnabledConfiguration(conf: RunConfigurationBase<*>): CoverageEnabledConfiguration {
    return super.createCoverageEnabledConfiguration(conf).apply {
      coverageRunner = getCoverageRunner()
    }
  }

  override fun createCoverageSuite(config: CoverageEnabledConfiguration): CoverageSuite? {
    JavaCoverageOptionsProvider.getInstance(config.configuration.project).coverageRunner = getCoverageRunner()
    return super.createCoverageSuite(config)
  }
}

/**
 * @see BazelJavaAgentCoverageEngine
 */
internal fun isJavaAgentCoverageApplicableTo(configuration: RunProfile?): Boolean {
  if (configuration !is BazelRunConfiguration) return false
  val handler = configuration.handler as? JvmTestHandler ?: return false
  // Only one target can be run with --script_path
  if (configuration.targets.size != 1) return false
  // Vanilla "bazel coverage" works with remote execution,
  // but BazelJavaAgentCoverageEngine doesn't as it uses -javaagent with a jar file from the local system.
  // So here we give the choice to the user
  return !handler.state.runWithBazel
}
