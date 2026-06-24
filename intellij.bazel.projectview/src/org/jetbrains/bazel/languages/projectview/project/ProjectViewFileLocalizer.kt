package org.jetbrains.bazel.languages.projectview.project

import com.intellij.openapi.util.io.findOrCreateDirectory
import com.intellij.openapi.util.io.toNioPathOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants.DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.DOT_BAZELBSP_DIR_NAME
import org.jetbrains.bazel.commons.constants.Constants.LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME
import org.jetbrains.bazel.commons.constants.Constants.PROJECT_VIEW_FILE_EXTENSION
import org.jetbrains.bazel.flow.open.ProjectViewFileUtils.projectViewTemplate
import org.jetbrains.bazel.utils.findVirtualFile
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.writeText

@ApiStatus.Internal
object ProjectViewFileLocalizer {
  private const val PROJECT_VIEW_FILE_SYSTEM_PROPERTY = "bazel.project.view.file.path"

  fun isDefaultProjectViewFile(projectIdentityFile: Path, rootDir: Path): Boolean {
    val parent = projectIdentityFile.parent ?: return false
    val fileName = projectIdentityFile.name
    return when {
      parent == rootDir -> fileName == DEFAULT_PROJECT_VIEW_FILE_NAME || fileName == LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME
      parent.name == DOT_BAZELBSP_DIR_NAME -> fileName == DEFAULT_PROJECT_VIEW_FILE_NAME
      else -> false
    }
  }

  fun pickProjectViewFileForProject(projectFile: Path, rootDir: Path): Path =
    useProjectFileAsProjectView(projectFile)
    ?: useProjectViewFileFromEnvVariable()
    ?: useProjectViewFileFromRootProjectDirectory(rootDir, DEFAULT_PROJECT_VIEW_FILE_NAME)
    ?: useProjectViewFileFromRootProjectDirectory(rootDir, LEGACY_DEFAULT_PROJECT_VIEW_FILE_NAME)
    ?: useDotBazelBspProjectViewFile(rootDir)

  private fun useProjectFileAsProjectView(projectFile: Path): Path ? {
    if (projectFile.isRegularFile() && projectFile.extension == PROJECT_VIEW_FILE_EXTENSION) {
      // Either user pointed to a project view file while opening project, or
      // used a "Load Project View Action", so plugin opened project based on a project view file
      return projectFile
    }
    // Project has been opened using a different Bazel-related file, so plugin needs to
    // find a default project view file to open, or create it.
    return null
  }

  private fun useProjectViewFileFromEnvVariable(): Path? {
    return System.getProperty(PROJECT_VIEW_FILE_SYSTEM_PROPERTY)?.toNioPathOrNull()
  }

  private fun useProjectViewFileFromRootProjectDirectory(rootDir: Path, fileName: String): Path? {
    val resolvedFilePath = rootDir.resolve(fileName)
    if (resolvedFilePath.isRegularFile()) {
      return resolvedFilePath
    }
    return null
  }

  private fun useDotBazelBspProjectViewFile(rootDir: Path): Path {
    val dotBazelBspDirectory = rootDir.findOrCreateDirectory(DOT_BAZELBSP_DIR_NAME)
    val projectViewFile = dotBazelBspDirectory.resolve(DEFAULT_PROJECT_VIEW_FILE_NAME)

    if (projectViewFile.isRegularFile()) {
      // Do not overwrite this file if it exists, because user may make his changes there.
      // According to the plugin documentation the idea is that .bazelbsp/.bazelproject is
      // a per-user file that isn't meant to be commited to the repo.
      // https://www.jetbrains.com/help/idea/2026.1/bazel-project-view.html
      return projectViewFile
    }

    val projectViewTemplate = projectViewTemplate(rootDir.findVirtualFile()!!)
    projectViewFile.writeText(projectViewTemplate)
    return projectViewFile
  }
}

