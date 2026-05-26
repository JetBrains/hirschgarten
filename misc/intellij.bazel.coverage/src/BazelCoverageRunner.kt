package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageLoadErrorReporter
import com.intellij.coverage.CoverageLoadingResult
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuite
import com.intellij.coverage.FailedCoverageLoadingResult
import com.intellij.coverage.SuccessCoverageLoadingResult
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.rootDir
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

internal class BazelCoverageRunner : CoverageRunner() {
  companion object {
    @JvmStatic
    fun getInstance(): BazelCoverageRunner = getInstance(BazelCoverageRunner::class.java)

    fun getNameInCoverageData(sourceFile: VirtualFile): String = sourceFile.path

    fun getFileByCoverageDataName(name: String): VirtualFile? = LocalFileSystem.getInstance().findFileByPath(name)

    private const val ID: String = "BazelCoverageRunner"
  }

  override fun loadCoverageData(
    coverageOutputDirectory: Path,
    suite: CoverageSuite?,
    reporter: CoverageLoadErrorReporter,
  ): CoverageLoadingResult {
    if (suite !is BazelCoverageSuite) {
      return FailedCoverageLoadingResult(BazelPluginBundle.message("coverage.bazel.loading.failed.invalid.suite"))
    }
    if (ApplicationManager.getApplication().isDispatchThread) {
      return ProgressManager.getInstance().runProcessWithProgressSynchronously<CoverageLoadingResult, RuntimeException>(
        { doLoadCoverageData(coverageOutputDirectory, suite, reporter) },
        BazelPluginBundle.message("coverage.bazel.loading.progress"),
        true,
        suite.project,
      )
    }
    return doLoadCoverageData(coverageOutputDirectory, suite, reporter)
  }

  private fun doLoadCoverageData(
    coverageOutputDirectory: Path,
    suite: BazelCoverageSuite,
    reporter: CoverageLoadErrorReporter,
  ): CoverageLoadingResult {
    val coverageFiles =
      if (coverageOutputDirectory.isDirectory()) {
        coverageOutputDirectory.listDirectoryEntries().filter { it.isRegularFile() }
      }
      else {
        emptyList()
      }
    if (coverageFiles.isEmpty()) {
      return FailedCoverageLoadingResult(BazelPluginBundle.message("coverage.bazel.loading.failed.no.files", coverageOutputDirectory))
    }
    val projectData = ProjectData()
    val projectRoot = suite.project.rootDir
    val coverageByFile = mutableMapOf<VirtualFile, MutableMap<Int, Long>>()

    val coverageConsumer =
      BazelCoverageLineConsumer { file, lineNumber, hits ->
        val lineHits = coverageByFile.getOrPut(file) { mutableMapOf() }
        lineHits.merge(lineNumber, hits) { oldHits, newHits -> maxOf(oldHits, newHits) }
      }

    val remainingCoverageFiles = coverageFiles.toMutableSet()
    BazelCoverageReportParser.ep.forEachExtensionSafe { parser ->
      val consumedCoverageFiles = parser.parse(remainingCoverageFiles.toList(), suite.project, projectRoot, reporter, coverageConsumer)
      remainingCoverageFiles.removeAll(consumedCoverageFiles)
    }

    for ((file, lineHits) in coverageByFile) {
      val lineData =
        lineHits.entries
          .sortedBy { it.key }
          .map { (lineNumber, hits) ->
            LineData(lineNumber, null).also {
              it.hits = hits.toCoverageHits()
            }
          }
      projectData.getOrCreateClassData(getNameInCoverageData(file)).setLines(lineData.toTypedArray())
    }

    return SuccessCoverageLoadingResult(projectData)
  }

  override fun getPresentableName(): String = "Bazel Coverage Runner"

  override fun getId(): String = ID

  override fun getDataFileExtension(): String = "lcov"

  override fun acceptsCoverageEngine(engine: CoverageEngine): Boolean = engine is BazelCoverageEngine
}

private fun Long.toCoverageHits(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
