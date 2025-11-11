package org.jetbrains.bazel.flow.open

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.PROJECT_VIEW_FILE_EXTENSION
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
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
  # For more options, see project view file documentation: https://www.jetbrains.com/help/idea/bazel-project-view.html
  
  derive_targets_from_directories: true
  directories: %s
  
  # Uncomment these lines to make indexing quicker:
  # import_depth: 0
  # import_ijars: true
  
  """.trimIndent()

private val OPEN_OPTIONS = arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

object ProjectViewFileUtils {
  fun calculateProjectViewFilePath(
    projectRootDir: VirtualFile,
    projectViewPath: Path?,
    overwrite: Boolean = false,
    bazelPackageDir: VirtualFile? = null,
  ): Path {
    val projectViewFilePath =
      projectViewPath?.toAbsolutePath()
        ?: System.getProperty(PROJECT_VIEW_FILE_SYSTEM_PROPERTY)?.let(Path::of)
        ?: calculateDefaultProjectViewFile(projectRootDir, overwrite)

    val content = createProjectViewFileContent(projectRootDir, bazelPackageDir)
    setProjectViewFileContent(projectViewFilePath, content, overwrite)

    return projectViewFilePath
  }

  fun projectViewTemplate(projectRootDir: VirtualFile): String {
    val path = projectRootDir.resolveFromRootOrRelative(PROJECTVIEW_DEFAULT_TEMPLATE_PATH)
    return path?.readText() ?: INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE
  }

  private fun calculateDefaultProjectViewFile(projectRootDir: VirtualFile, overwrite: Boolean): Path {
    val rootDir = projectRootDir.toNioPath()
    return (if (overwrite) null else calculateLegacyManagedProjectViewFile(rootDir))
      ?: calculateProjectViewFileInCurrentDirectory(rootDir.resolve(Constants.DOT_BAZELBSP_DIR_NAME))
  }

  private fun calculateLegacyManagedProjectViewFile(projectRootDir: Path): Path? {
    val projectViewFilesFromRoot =
      Files
        .list(projectRootDir)
        .filter { it.isRegularFile() }
        .filter { it.extension == PROJECT_VIEW_FILE_EXTENSION }
        .toList()

    return projectViewFilesFromRoot.firstOrNull { it.name == DEFAULT_PROJECT_VIEW_FILE_NAME }
      ?: projectViewFilesFromRoot.firstOrNull { it.name == LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME }
      ?: projectViewFilesFromRoot.firstOrNull()
  }

  /**
   * Supports the project view file with legacy name [LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME] if it exists
   */
  private fun calculateProjectViewFileInCurrentDirectory(directory: Path): Path {
    val legacyProjectViewFile = directory.resolve(LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME)
    if (legacyProjectViewFile.isRegularFile()) return legacyProjectViewFile
    return directory.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)
  }

  private fun createProjectViewFileContent(rootDir: VirtualFile, bazelPackageDir: VirtualFile?): String {
    val projectRoot = rootDir.toNioPath()
    val realizedBazelPackageDir = bazelPackageDir?.toNioPath() ?: projectRoot
    val relativePath = calculateRelativePathForInferredDirectory(projectRoot, realizedBazelPackageDir)
    return projectViewTemplate(rootDir).format(relativePath)
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
