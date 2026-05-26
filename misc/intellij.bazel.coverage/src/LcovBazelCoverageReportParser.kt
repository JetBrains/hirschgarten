package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageLoadErrorReporter
import com.intellij.coverage.lcov.LcovSerializationUtils
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import java.nio.file.Path

private val LOG = logger<LcovBazelCoverageReportParser>()

@ApiStatus.Internal
class LcovBazelCoverageReportParser : BazelCoverageReportParser {
  override fun parse(
    coverageFiles: List<Path>,
    project: Project,
    projectRoot: VirtualFile,
    reporter: CoverageLoadErrorReporter,
    consumer: BazelCoverageLineConsumer,
  ): Collection<Path> {
    if (coverageFiles.isEmpty()) return emptyList()

    val consumedCoverageFiles = mutableListOf<Path>()
    for (coverageFile in coverageFiles) {
      try {
        val lcovCoverageReport = LcovSerializationUtils.readLCOVFromPaths(listOf(coverageFile))
        if (lcovCoverageReport.info.isEmpty()) {
          reportWarning(reporter, BazelPluginBundle.message("coverage.lcov.report.empty", coverageFile))
          continue
        }
        for ((filePath, lineHints) in lcovCoverageReport.info) {
          val file = projectRoot.resolveFromRootOrRelative(filePath) ?: continue
          lineHints.forEach { lineHint ->
            consumer.addCoverage(file, lineHint.lineNumber, lineHint.hits.toLong())
          }
        }
        consumedCoverageFiles.add(coverageFile)
      }
      catch (e: Exception) {
        if (e is ControlFlowException) throw e
        reportWarning(reporter, BazelPluginBundle.message("coverage.lcov.report.failed", coverageFile), e)
      }
    }
    return consumedCoverageFiles
  }
}

private fun reportWarning(reporter: CoverageLoadErrorReporter, message: String, exception: Exception? = null) {
  LOG.warn(message, exception)
  reporter.reportWarning(message, exception)
}
