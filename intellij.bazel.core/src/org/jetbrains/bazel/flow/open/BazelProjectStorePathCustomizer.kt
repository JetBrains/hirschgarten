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
import org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.pickProjectViewFileForProject
import org.jetbrains.bazel.utils.isRealPathInside
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

  private fun getBazelStoreDirectoryPath(openedFile: Path): BazelProjectStoreDescriptor? {
    log.info("Computing BazelProjectStoreDescriptor for file: $openedFile")

    val (rootDir, projectIdentityFile) = ResolvedProjectRoot.from(openedFile = openedFile) ?: return null
    val projectViewFile = pickProjectViewFileForProject(projectIdentityFile, rootDir)
    val dotIdeaPath = selectDotIdeaPath(rootDir, projectViewFile)

    log.trace { "rootDir = $rootDir, projectIdentityFile = $projectIdentityFile, projectViewFile = $projectViewFile, dotIdea = $dotIdeaPath" }
    return BazelProjectStoreDescriptor(
      projectIdentityFile = projectIdentityFile,
      dotIdea = dotIdeaPath,
      historicalProjectBasePath = rootDir,
      projectViewFile = projectViewFile,
    )
  }

  /**
   * Returns the `.idea` directory from the project view, or `<root>/.idea`.
   * The real path of the directory from the project view must be inside the real project root.
   * The function does not check `<root>/.idea`, like the platform does not check it for other projects.
   */
  private fun selectDotIdeaPath(projectRootPath: Path, projectViewPath: Path): Path {
    val defaultDotIdeaPath = projectRootPath.resolve(DIRECTORY_STORE_FOLDER)
    val defaultProject = ProjectManager.getInstance().defaultProject
    val projectView = ProjectViewFactory.fromOrNull(defaultProject, projectViewPath, projectRootPath)
    val dotIdeaPath = projectView?.dotIdeaDirectoryLocation?.let { projectRootPath.resolve(it).normalize() } ?: return defaultDotIdeaPath
    if (!dotIdeaPath.isRealPathInside(projectRootPath)) {
      log.warn("The .idea directory $dotIdeaPath is outside of the project root $projectRootPath, using $defaultDotIdeaPath")
      return defaultDotIdeaPath
    }
    return dotIdeaPath
  }

  companion object {
    private val log = logger<BazelProjectStorePathCustomizer>()
  }
}
