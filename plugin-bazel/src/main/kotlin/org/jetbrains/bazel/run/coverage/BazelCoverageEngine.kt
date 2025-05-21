package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageAnnotator
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageFileProvider
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuite
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.view.CoverageViewExtension
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.target.targetUtils
import java.io.File

class BazelCoverageEngine : CoverageEngine() {
  companion object {
    @JvmStatic
    fun getInstance(): BazelCoverageEngine = EP_NAME.findExtensionOrFail(BazelCoverageEngine::class.java)
  }

  override fun getPresentableText(): @NlsActions.ActionText String? = "Bazel Coverage"

  override fun isApplicableTo(configuration: RunConfigurationBase<*>): Boolean {
    if (configuration !is BazelRunConfiguration) return false
    val targetUtils = configuration.project.targetUtils
    return configuration.targets.all { targetUtils.getBuildTargetForLabel(it)?.kind?.ruleType == RuleType.TEST }
  }

  override fun createCoverageEnabledConfiguration(configuration: RunConfigurationBase<*>): CoverageEnabledConfiguration =
    BazelCoverageEnabledConfiguration(configuration)

  override fun createCoverageSuite(config: CoverageEnabledConfiguration): CoverageSuite? =
    BazelCoverageSuite(
      config.createSuiteName(),
      config.configuration.project,
      BazelCoverageRunner.getInstance(),
      config.createFileProvider(),
      config.createTimestamp(),
    )

  override fun createCoverageSuite(
    name: String,
    project: Project,
    runner: CoverageRunner,
    fileProvider: CoverageFileProvider,
    timestamp: Long
  ): CoverageSuite {
    return BazelCoverageSuite(name, project, runner, fileProvider, timestamp)
  }

  @Deprecated("Deprecated in Java")
  override fun createCoverageSuite(
    runner: CoverageRunner,
    name: String,
    coverageDataFileProvider: CoverageFileProvider,
    config: CoverageEnabledConfiguration,
  ): CoverageSuite? = BazelCoverageSuite(name, config.configuration.project, runner, config.createFileProvider(), config.createTimestamp())

  override fun createEmptyCoverageSuite(coverageRunner: CoverageRunner): CoverageSuite? = BazelCoverageSuite()

  override fun getCoverageAnnotator(project: Project): CoverageAnnotator = BazelCoverageAnnotator.getInstance(project)

  override fun getQualifiedName(outputFile: File, sourceFile: PsiFile): String =
    BazelCoverageRunner.getNameInCoverageData(sourceFile.virtualFile)

  override fun coverageProjectViewStatisticsApplicableTo(fileOrDir: VirtualFile): Boolean = true

  override fun createCoverageViewExtension(project: Project, suiteBundle: CoverageSuitesBundle): CoverageViewExtension =
    BazelCoverageViewExtension(project, getCoverageAnnotator(project), suiteBundle)
}
