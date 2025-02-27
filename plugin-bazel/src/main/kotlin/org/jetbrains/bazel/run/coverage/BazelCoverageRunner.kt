package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuite
import com.intellij.coverage.lcov.LcovSerializationUtils
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import org.jetbrains.bazel.config.rootDir
import java.io.File

class BazelCoverageRunner : CoverageRunner() {
  companion object {
    @JvmStatic
    fun getInstance(): BazelCoverageRunner = getInstance(BazelCoverageRunner::class.java)

    fun getNameInCoverageData(sourceFile: VirtualFile): String = sourceFile.path

    fun getFileByCoverageDataName(name: String): VirtualFile? = LocalFileSystem.getInstance().findFileByPath(name)

    private const val ID: String = "BazelCoverageRunner"
  }

  override fun loadCoverageData(coverageOutputDirectory: File, suite: CoverageSuite?): ProjectData? {
    if (suite !is BazelCoverageSuite) return null
    val coverageFiles = coverageOutputDirectory.listFiles()
    if (coverageFiles.isEmpty()) return null
    val lcovCoverageReport = LcovSerializationUtils.readLCOV(coverageFiles.toList())
    val projectData = ProjectData()
    val projectRoot = suite.project.rootDir

    for ((filePath, lineHints) in lcovCoverageReport.info) {
      val file = projectRoot.resolveFromRootOrRelative(filePath) ?: continue
      val lineData =
        lineHints.map { lineHint ->
          LineData(lineHint.lineNumber, null).also {
            it.hits = lineHint.hits
          }
        }
      projectData.getOrCreateClassData(getNameInCoverageData(file)).setLines(lineData.toTypedArray())
    }

    return projectData
  }

  override fun getPresentableName(): String = "Bazel Coverage Runner"

  override fun getId(): String = ID

  override fun getDataFileExtension(): String = "lcov"

  override fun acceptsCoverageEngine(engine: CoverageEngine): Boolean = engine is BazelCoverageEngine
}
