package org.jetbrains.bazel.flow.open

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.settings.bazelProjectSettings
import org.jetbrains.bsp.bazel.commons.Constants.DOT_BAZELBSP_DIR_NAME
import org.jetbrains.bsp.bazel.install.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bsp.bazel.install.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.plugins.bsp.config.rootDir
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

private const val PROJECT_VIEW_FILE_SYSTEM_PROPERTY = "bazel.project.view.file.path"

private val INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE =
  """
  derive_targets_from_directories: true
  directories: %s
  import_depth: 0
  
  # For more options, see https://github.com/JetBrains/hirschgarten/blob/main/server/executioncontext/projectview/README.md
  """.trimIndent()

private val OPEN_OPTIONS = arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

internal object ProjectViewFileUtils {
  fun calculateProjectViewFilePath(
    project: Project,
    generateContentIfNotExists: Boolean,
    bazelPackageDir: Path?,
  ): Path {
    val projectViewFilePath = project.bazelProjectSettings.projectViewPath?.toAbsolutePath() ?: calculateDefaultProjectViewFile(project)
    if (generateContentIfNotExists) {
      setProjectViewFileContentIfNotExists(projectViewFilePath, project, bazelPackageDir)
    }
    return projectViewFilePath
  }

  private fun calculateDefaultProjectViewFile(project: Project): Path =
    System.getProperty(PROJECT_VIEW_FILE_SYSTEM_PROPERTY)?.let { Path(it) }
      ?: calculateManagedProjectViewFile(project)
      ?: calculateProjectViewFileInCurrentDirectory(project.rootDir.toNioPath().resolve(DOT_BAZELBSP_DIR_NAME))

  private fun calculateManagedProjectViewFile(project: Project): Path? =
    project.rootDir
      .toNioPath()
      .resolve("tools")
      .resolve("intellij")
      .resolve(".managed.bazelproject")
      .takeIf { it.isRegularFile() }

  /**
   * Supports the project view file with legacy name [LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME] if it exists
   */
  private fun calculateProjectViewFileInCurrentDirectory(directory: Path): Path {
    if (!directory.isDirectory()) {
      // if this turns out to be a file
      directory.deleteIfExists()
      directory.createDirectories()
    }
    val legacyProjectViewFile = directory.resolve(LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME)
    if (legacyProjectViewFile.isRegularFile()) return legacyProjectViewFile
    return directory.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)
  }

  private fun setProjectViewFileContentIfNotExists(
    projectViewFilePath: Path,
    project: Project,
    bazelPackageDir: Path?,
  ) {
    val projectRoot = project.rootDir.toNioPath()
    val realizedBazelPackageDir = bazelPackageDir ?: projectRoot
    val relativePath = calculateRelativePathForInferredDirectory(projectRoot, realizedBazelPackageDir)
    val content = INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE.format(relativePath)
    setProjectViewFileContentIfNotExists(projectViewFilePath, content)
  }

  private fun setProjectViewFileContentIfNotExists(projectViewFilePath: Path, content: String) {
    if (projectViewFilePath.isRegularFile()) return
    // just in case there is a directory with the same name existing
    projectViewFilePath.deleteIfExists()
    projectViewFilePath.writeText(content, options = OPEN_OPTIONS)
  }

  private fun calculateRelativePathForInferredDirectory(projectRoot: Path, bazelPackageDir: Path): String {
    val relativePath = bazelPackageDir.relativeTo(projectRoot).toString()
    if (relativePath.isBlank()) return "."
    return relativePath
  }
}
