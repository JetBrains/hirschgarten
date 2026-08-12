package org.jetbrains.bazel.flow.open

import com.intellij.configurationStore.ProjectStoreDescriptor
import com.intellij.configurationStore.ProjectStorePathCustomizer
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project.DIRECTORY_STORE_FOLDER
import com.intellij.openapi.project.ProjectManager
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.languages.projectview.ProjectViewFactory
import org.jetbrains.bazel.languages.projectview.dotIdeaDirectoryLocation
import org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.isDefaultProjectViewFile
import org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.pickProjectViewFileForProject
import java.nio.file.Path

internal class BazelProjectStorePathCustomizer : ProjectStorePathCustomizer {
  override fun getStoreDirectoryPath(projectRoot: Path): ProjectStoreDescriptor? {
    if (!isFileSupported(projectRoot)) return null
    return getBazelStoreDirectoryPath(projectRoot)
  }

  private fun isFileSupported(fileBeingOpen: Path): Boolean {
    // do not use isDirectory, as we also want to check that the file exists
    return fileBeingOpen.hasNameOf(*Constants.SUPPORTED_CONFIG_FILE_NAMES) ||
           fileBeingOpen.hasExtensionOf(*Constants.SUPPORTED_EXTENSIONS)
  }

  private fun getBazelStoreDirectoryPath(fileBeingOpen: Path): BazelProjectStoreDescriptor {
    log.info("Computing BazelProjectStoreDescriptor for file: $fileBeingOpen")

    val projectRootPath = findProjectRootPath(fileBeingOpen)
    val projectIdentityFilePath = selectProjectIdentityFilePath(projectRootPath, fileBeingOpen)
    val projectViewFile = pickProjectViewFileForProject(projectIdentityFilePath, projectRootPath)
    val dotIdeaPath = selectDotIdeaPath(projectRootPath, projectViewFile)

    log.trace { "projectRootPath = $projectRootPath, projectIdentityFilePath = $projectIdentityFilePath, projectViewFile = $projectViewFile, dotIdea = $dotIdeaPath" }
    return BazelProjectStoreDescriptor(
      projectIdentityFile = projectIdentityFilePath,
      dotIdea = dotIdeaPath,
      historicalProjectBasePath = projectRootPath,
      projectViewFile = projectViewFile,
    )
  }

  private fun selectProjectIdentityFilePath(projectRoot: Path, fileBeingOpen: Path): Path {
    // The purpose of this function is to improve the user's experience with project history.
    // When the file being opened is one of the project view files that the plugin can automatically pick
    // during project opening (the "default project view"), then the project identity file will become MODULE.bazel.
    // This way, we won't create multiple history entries that will ultimately open the same project view.
    if (isDefaultProjectViewFile(fileBeingOpen, projectRoot)) {
      projectRoot.workspaceFile?.let { return it }
    }
    return fileBeingOpen
  }

  private fun findProjectRootPath(fileBeingOpen: Path): Path =
    findProjectFolderFromFile(fileBeingOpen) ?: fileBeingOpen.parent

  private fun selectDotIdeaPath(projectRootPath: Path, projectViewPath: Path): Path {
    val defaultProject = ProjectManager.getInstance().defaultProject
    val projectView = ProjectViewFactory.fromOrNull(defaultProject, projectViewPath, projectRootPath)
    return projectView
      ?.dotIdeaDirectoryLocation
      ?.let(projectRootPath::resolve)
      ?: projectRootPath.defaultDotIdeaDirectory()
  }

  private fun Path.defaultDotIdeaDirectory(): Path = resolve(DIRECTORY_STORE_FOLDER)

  companion object {
    private val log = logger<BazelProjectStorePathCustomizer>()
  }
}
