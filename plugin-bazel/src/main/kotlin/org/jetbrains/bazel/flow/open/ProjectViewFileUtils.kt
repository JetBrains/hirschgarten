package org.jetbrains.bazel.flow.open

// TODO rewrite with no VirtualFile operations!
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import org.jetbrains.bazel.commons.constants.Constants
import java.nio.file.Files.isDirectory
import java.nio.file.Files.isRegularFile
import java.nio.file.Files.list
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.writeText

private const val PROJECT_VIEW_FILE_SYSTEM_PROPERTY = "bazel.project.view.file.path"

private const val PROJECTVIEW_DEFAULT_TEMPLATE_PATH = "tools/intellij/.managed.bazelproject"

private val INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE =
  """
  # This project view file may be overwritten by the Bazel plugin.
  # To use it as default, place it in the Bazel module or workspace root directory.
  # For more options, see project view file documentation
  
  derive_targets_from_directories: true
  directories: %s
  """.trimIndent()

private val OPEN_OPTIONS = arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

object ProjectViewFileUtils {
  fun calculateProjectViewFilePath(
    projectRootDir: VirtualFile,
    projectViewPath: Path?,
    overwrite: Boolean = false,
    bazelPackageDir: VirtualFile? = null,
  ): Path {
    // todo should be the actual parameter
    val projectRootPath = projectRootDir.toNioPath()

    val projectViewFilePath =
      projectViewPath?.toAbsolutePath()
        ?: System.getProperty(PROJECT_VIEW_FILE_SYSTEM_PROPERTY)?.let(Path::of)
        ?: (if (overwrite) null else calculateLegacyManagedProjectViewFile(projectRootPath))
        ?: calculateProjectViewFileInCurrentDirectory(projectRootPath.resolve(Constants.DOT_BAZELBSP_DIR_NAME))

    val content = createProjectViewFileContent(projectRootDir, bazelPackageDir)
    setProjectViewFileContent(projectViewFilePath, content, overwrite)

    return projectViewFilePath
  }

  fun projectViewTemplate(projectRootDir: VirtualFile): String {
    val path = projectRootDir.resolveFromRootOrRelative(PROJECTVIEW_DEFAULT_TEMPLATE_PATH)
    return path?.readText() ?: INFERRED_DIRECTORY_PROJECT_VIEW_TEMPLATE
  }

  private fun calculateLegacyManagedProjectViewFile(projectRootDir: Path): Path? {
    val projectViewFilesFromRoot =
      list(projectRootDir)
        .filter { isRegularFile(it) }
        .filter { it.extension == Constants.PROJECT_VIEW_FILE_EXTENSION }
        .toList()

    return projectViewFilesFromRoot.firstOrNull { it.name == Constants.DEFAULT_PROJECT_VIEW_FILE_NAME }
      ?: projectViewFilesFromRoot.firstOrNull { it.name == Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME }
      ?: projectViewFilesFromRoot.firstOrNull()
  }

  /**
   * Supports the project view file with legacy name [Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME] if it exists
   */
  private fun calculateProjectViewFileInCurrentDirectory(directory: Path): Path {
    val legacyProjectViewFile = directory.resolve(Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME)
    return if (isRegularFile(legacyProjectViewFile)) legacyProjectViewFile
    else directory.resolve(Constants.DEFAULT_PROJECT_VIEW_FILE_NAME)
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
    if (!isDirectory(projectViewFilePathDirectory)) {
      projectViewFilePathDirectory.deleteIfExists()
      projectViewFilePathDirectory.createDirectories()
    }
    if (!overwrite && isRegularFile(projectViewFilePath)) return
    projectViewFilePath.deleteIfExists()
    projectViewFilePath.writeText(content, options = OPEN_OPTIONS)
  }

  private fun calculateRelativePathForInferredDirectory(projectRoot: Path, bazelPackageDir: Path): String {
    val relativePath = bazelPackageDir.relativeTo(projectRoot).toString()
    if (relativePath.isBlank()) return "."
    return relativePath
  }
}
