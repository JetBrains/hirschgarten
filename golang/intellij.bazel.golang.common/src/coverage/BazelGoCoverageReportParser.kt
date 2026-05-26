package org.jetbrains.bazel.golang.coverage

import com.intellij.coverage.CoverageLoadErrorReporter
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.run.coverage.BazelCoverageLineConsumer
import org.jetbrains.bazel.run.coverage.BazelCoverageReportParser
import org.jetbrains.bazel.workspacemodel.entities.ImportPathId
import java.nio.file.Path

private val LOG = logger<BazelGoCoverageReportParser>()

internal class BazelGoCoverageReportParser : BazelCoverageReportParser {
  override fun parse(
    coverageFiles: List<Path>,
    project: Project,
    projectRoot: VirtualFile,
    reporter: CoverageLoadErrorReporter,
    consumer: BazelCoverageLineConsumer,
  ): Collection<Path> {
    if (coverageFiles.isEmpty()) return emptyList()

    val consumedCoverageFiles = mutableListOf<Path>()
    val warningReporter = { message: String ->
      LOG.warn(message)
      reporter.reportWarning(message)
    }
    val goSourceResolver = GoCoverageSourceResolver(project, projectRoot, warningReporter)
    val unresolvedFilePaths = mutableSetOf<String>()
    for (coverageFile in coverageFiles) {
      val consumed =
        GoCoverageReportParser.parse(
          coverageFile,
          onMalformedLine = { line ->
            warningReporter(BazelPluginBundle.message("coverage.go.report.malformed.line", line))
          },
        ) coverageLine@ { filePath, lineNumber, hits ->
          val file = goSourceResolver.resolve(filePath)
          if (file == null) {
            if (unresolvedFilePaths.add(filePath)) {
              warningReporter(BazelPluginBundle.message("coverage.go.report.unresolved.file", filePath))
            }
            return@coverageLine
          }
          consumer.addCoverage(file, lineNumber, hits)
        }
      if (consumed) consumedCoverageFiles.add(coverageFile)
    }
    return consumedCoverageFiles
  }
}

private class GoCoverageSourceResolver(
  private val project: Project,
  private val projectRoot: VirtualFile,
  private val warningReporter: (String) -> Unit,
) {
  private val resolvedFiles = mutableMapOf<String, VirtualFile?>()

  fun resolve(filePath: String): VirtualFile? {
    if (resolvedFiles.containsKey(filePath)) return resolvedFiles[filePath]
    val file = resolveGoSource(filePath) ?: projectRoot.resolveFromRootOrRelative(filePath)
    resolvedFiles[filePath] = file
    return file
  }

  private fun resolveGoSource(filePath: String): VirtualFile? {
    val importPath = filePath.substringBeforeLast('/', missingDelimiterValue = "")
    val fileName = filePath.substringAfterLast('/')
    val goPackage = project.workspaceModel.currentSnapshot.resolve(ImportPathId(importPath)) ?: return null
    var result: VirtualFile? = null
    for (source in goPackage.sources) {
      ProgressManager.checkCanceled()
      val file = source.virtualFile ?: continue
      if (file.name != fileName) continue
      val existingFile = result
      if (existingFile == null) {
        result = file
      }
      else if (existingFile != file) {
        warningReporter(BazelPluginBundle.message("coverage.go.report.path.collision", filePath, existingFile.path, file.path))
      }
    }
    return result
  }
}
