package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageLoadErrorReporter
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.writeText

@TestApplication
class LcovBazelCoverageReportParserTest {
  companion object {
    private val projectFixture = projectFixture()
  }

  @Test
  fun `broken report does not discard coverage from valid sibling`(@TempDir tempDir: Path) {
    val sourcePath = tempDir.resolve("source.go").createFile()
    val validReport = tempDir.resolve("valid.lcov")
    validReport.writeText("SF:source.go\nDA:1,7\nend_of_record\n")
    val brokenReport = tempDir.resolve("broken.lcov")
    brokenReport.writeText("DA:1,1\n")
    val projectRoot = checkNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(tempDir))
    val reporter = RecordingCoverageLoadErrorReporter()
    val coverage = mutableListOf<Triple<String, Int, Long>>()

    val consumedFiles =
      LcovBazelCoverageReportParser().parse(
        listOf(validReport, brokenReport),
        projectFixture.get(),
        projectRoot,
        reporter,
      ) { file, lineNumber, hits ->
        coverage.add(Triple(file.path, lineNumber, hits))
      }

    consumedFiles shouldBe listOf(validReport)
    coverage shouldBe listOf(Triple(sourcePath.toString(), 1, 7L))
    reporter.warnings.size shouldBe 1
    reporter.warnings.single().contains(brokenReport.toString()) shouldBe true
  }
}

private class RecordingCoverageLoadErrorReporter : CoverageLoadErrorReporter {
  val warnings = mutableListOf<String>()

  override fun reportError(reason: String) = Unit

  override fun reportError(e: Exception) = Unit

  override fun reportWarning(reason: String, e: Exception?) {
    warnings.add(reason)
  }

  override fun reportWarning(e: Exception) {
    warnings.add(e.message.orEmpty())
  }
}
