package org.jetbrains.bazel.golang.coverage

import com.intellij.coverage.CoverageLoadErrorReporter
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.utils.vfs.createFile
import com.intellij.testFramework.workspaceModel.updateProjectModel
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.run.coverage.BazelCoverageReportParser
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelGoPackageEntity
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.writeText

@TestApplication
class BazelGoCoverageReportParserTest {
  private val tempDirFixture = tempPathFixture()
  private val tempDir by tempDirFixture
  private val projectFixture = projectFixture(pathFixture = tempDirFixture, openAfterCreation = true)
  private val project by projectFixture

  @Test
  fun `parser resolves package sources through workspace model and falls back to project root`() {
    val packageSource = createSource("generated", "calculator.go")
    val fallbackSource = createSource("", "fallback.go")
    addGoPackage("github.com/example/project/pkg", packageSource)
    val report = tempDir.resolve("coverage.out")
    report.writeText(
      """
        mode: atomic
        github.com/example/project/pkg/calculator.go:2.1,2.2 1 3
        fallback.go:5.1,5.2 1 1
      """.trimIndent(),
    )

    val result = parse(report)

    result.consumedFiles shouldBe listOf(report)
    result.coverage shouldBe
      listOf(
        Triple(packageSource, 2, 3L),
        Triple(fallbackSource, 5, 1L),
      )
    result.warnings shouldBe emptyList()
  }

  @Test
  fun `parser reports package sources with colliding coverage paths`() {
    val firstSource = createSource("first", "duplicate.go")
    val secondSource = createSource("second", "duplicate.go")
    addGoPackage("github.com/example/project/collision", firstSource, secondSource)
    val report = tempDir.resolve("collision.out")
    report.writeText(
      """
        mode: atomic
        github.com/example/project/collision/duplicate.go:7.1,7.2 1 4
      """.trimIndent(),
    )

    val result = parse(report)

    result.coverage shouldBe listOf(Triple(firstSource, 7, 4L))
    result.warnings.size shouldBe 1
    result.warnings.single().contains(firstSource.path) shouldBe true
    result.warnings.single().contains(secondSource.path) shouldBe true
  }

  private fun createSource(directory: String, fileName: String): VirtualFile =
    runWriteAction {
      val parent = if (directory.isEmpty()) projectRoot else projectRoot.findOrCreateDirectory(directory)
      parent.createFile(fileName)
    }

  private fun addGoPackage(importPath: String, vararg sources: VirtualFile) {
    val workspaceModel = project.workspaceModel
    val virtualFileUrlManager = workspaceModel.getVirtualFileUrlManager()
    runWriteAction {
      workspaceModel.updateProjectModel("add Go package $importPath") { storage ->
        storage.addEntity(
          BazelGoPackageEntity(
            importPath = importPath,
            sources = sources.map { it.toVirtualFileUrl(virtualFileUrlManager) },
            entitySource = BazelDummyEntitySource,
          ),
        )
      }
    }
  }

  private fun parse(report: Path): ParseResult {
    val reporter = RecordingCoverageLoadErrorReporter()
    val coverage = mutableListOf<Triple<VirtualFile, Int, Long>>()
    val parser = BazelCoverageReportParser.ep.extensionList.single { it.javaClass.simpleName == "BazelGoCoverageReportParser" }
    val consumedFiles =
      parser.parse(
        listOf(report),
        project,
        projectRoot,
        reporter,
      ) { file, lineNumber, hits ->
        coverage.add(Triple(file, lineNumber, hits))
      }
    return ParseResult(consumedFiles, coverage, reporter.warnings)
  }

  private val projectRoot: VirtualFile
    get() = checkNotNull(tempDir.refreshAndFindVirtualDirectory())
}

private data class ParseResult(
  val consumedFiles: Collection<Path>,
  val coverage: List<Triple<VirtualFile, Int, Long>>,
  val warnings: List<String>,
)

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
