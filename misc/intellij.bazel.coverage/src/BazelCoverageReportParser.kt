package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageLoadErrorReporter
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
interface BazelCoverageReportParser {
  /**
   * Parses matching coverage files and returns files that should not be passed to later parsers.
   */
  fun parse(
    coverageFiles: List<Path>,
    project: Project,
    projectRoot: VirtualFile,
    reporter: CoverageLoadErrorReporter,
    consumer: BazelCoverageLineConsumer,
  ): Collection<Path>

  companion object {
    val ep = ExtensionPointName<BazelCoverageReportParser>("org.jetbrains.bazel.bazelCoverageReportParser")
  }
}

@ApiStatus.Internal
fun interface BazelCoverageLineConsumer {
  fun addCoverage(file: VirtualFile, lineNumber: Int, hits: Long)
}
