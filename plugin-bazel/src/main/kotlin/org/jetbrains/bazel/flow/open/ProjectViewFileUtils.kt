package org.jetbrains.bazel.flow.open

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.DOT_BAZELBSP_DIR_NAME
import org.jetbrains.bazel.commons.constants.Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.PROJECT_VIEW_FILE_EXTENSION
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

private const val PROJECT_VIEW_FILE_SYSTEM_PROPERTY = "bazel.project.view.file.path"

private const val PROJECTVIEW_DEFAULT_TEMPLATE_PATH = "tools/intellij/.managed.bazelproject"

private val INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE =
  """
  # This project view file may be overwritten by the Bazel plugin.
  # To use it as default, place it in the Bazel module or workspace root directory.
  # For more options, see https://github.com/JetBrains/hirschgarten/blob/main/commons/src/main/kotlin/org/jetbrains/bazel/projectview/README.md
  
  derive_targets_from_directories: true
  directories: %s
  """.trimIndent()

private val OPEN_OPTIONS = arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

object ProjectViewFileUtils {
  fun calculateProjectViewFilePath(
    project: Project,
    generateContent: Boolean,
    overwrite: Boolean,
    bazelPackageDir: Path?,
  ): Path {
    val projectViewFilePath =
      project.bazelProjectSettings.projectViewPath?.toAbsolutePath() ?: calculateDefaultProjectViewFile(project, overwrite)
    if (generateContent) {
      setProjectViewFileContent(projectViewFilePath, project, bazelPackageDir, overwrite)
    }
    return projectViewFilePath
  }

  fun projectViewTemplate(project: Project): String {
    val path = project.rootDir.resolveFromRootOrRelative(PROJECTVIEW_DEFAULT_TEMPLATE_PATH)
    return path?.readText() ?: INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE
  }

  private fun calculateDefaultProjectViewFile(project: Project, overwrite: Boolean): Path {
    val projectViewFileInSystemProperty = System.getProperty(PROJECT_VIEW_FILE_SYSTEM_PROPERTY)
    if (projectViewFileInSystemProperty != null) return Path(projectViewFileInSystemProperty)
    val dotBazelBspProjectViewFile = calculateProjectViewFileInCurrentDirectory(project.rootDir.toNioPath().resolve(DOT_BAZELBSP_DIR_NAME))
    return calculateLegacyManagedProjectViewFile(project).takeIf { !overwrite }
      ?: dotBazelBspProjectViewFile
  }

  private fun calculateLegacyManagedProjectViewFile(project: Project): Path? {
    val projectViewFilesFromRoot =
      Files
        .list(project.rootDir.toNioPath())
        .filter {
          it.isRegularFile() &&
            it.extension == PROJECT_VIEW_FILE_EXTENSION
        }.toList()
    val dotProjectViewFile = projectViewFilesFromRoot.firstOrNull { it.name == DEFAULT_PROJECT_VIEW_FILE_NAME }
    val legacyProjectViewFile = projectViewFilesFromRoot.firstOrNull { it.name == LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME }
    return dotProjectViewFile ?: legacyProjectViewFile ?: projectViewFilesFromRoot.firstOrNull()
  }

  /**
   * Supports the project view file with legacy name [LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME] if it exists
   */
  private fun calculateProjectViewFileInCurrentDirectory(directory: Path): Path {
    val legacyProjectViewFile = directory.resolve(LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME)
    if (legacyProjectViewFile.isRegularFile()) return legacyProjectViewFile
    return directory.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)
  }

  private fun setProjectViewFileContent(
    projectViewFilePath: Path,
    project: Project,
    bazelPackageDir: Path?,
    overwrite: Boolean,
  ) {
    val projectRoot = project.rootDir.toNioPath()
    val realizedBazelPackageDir = bazelPackageDir ?: projectRoot
    val relativePath = calculateRelativePathForInferredDirectory(projectRoot, realizedBazelPackageDir)
    val content = projectViewTemplate(project).format(relativePath)
    setProjectViewFileContent(projectViewFilePath, content, overwrite)
  }

  private fun setProjectViewFileContent(
    projectViewFilePath: Path,
    content: String,
    overwrite: Boolean,
  ) {
    val projectViewFilePathDirectory = projectViewFilePath.parent
    if (!projectViewFilePathDirectory.isDirectory()) {
      projectViewFilePathDirectory.deleteIfExists()
      projectViewFilePathDirectory.createDirectories()
    }
    if (!overwrite && projectViewFilePath.isRegularFile()) return
    projectViewFilePath.deleteIfExists()
    projectViewFilePath.writeText(content, options = OPEN_OPTIONS)
  }

  private fun calculateRelativePathForInferredDirectory(projectRoot: Path, bazelPackageDir: Path): String {
    val relativePath = bazelPackageDir.relativeTo(projectRoot).toString()
    if (relativePath.isBlank()) return "."
    return relativePath
  }
}
