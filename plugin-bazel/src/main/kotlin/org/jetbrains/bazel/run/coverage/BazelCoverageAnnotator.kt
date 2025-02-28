package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.SimpleCoverageAnnotator
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.utils.SourceType
import java.io.File

private const val AVERAGE_LINE_COUNT_IN_FILE = 100

@Service(Service.Level.PROJECT)
class BazelCoverageAnnotator(project: Project) : SimpleCoverageAnnotator(project) {
  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelCoverageAnnotator = project.service()
  }

  override fun getRoots(
    project: Project,
    dataManager: CoverageDataManager,
    suite: CoverageSuitesBundle,
  ): Array<VirtualFile> = BazelCoverageSuite.getRoots(suite).toTypedArray()

  override fun fillInfoForUncoveredFile(file: File): FileCoverageInfo? {
    // Only include Java/Kotlin/etc. files in the total count
    if (SourceType.fromExtension(file.extension) == null) return null

    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return null
    if (runReadAction {
        ProjectFileIndex.getInstance(project).isInTestSourceContent(virtualFile) ||
          !ProjectFileIndex.getInstance(project).isInSource(virtualFile)
      }
    ) {
      return null
    }

    return FileCoverageInfo().apply {
      coveredLineCount = 0
      // Best-effort attempt without actually reading the file from disk (slow!).
      // Even if we were to read it and count the lines, the number of statements is not the same as the number of physical lines.
      totalLineCount = AVERAGE_LINE_COUNT_IN_FILE
    }
  }
}
