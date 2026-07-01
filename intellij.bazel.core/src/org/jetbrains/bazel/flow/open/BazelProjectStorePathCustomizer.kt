package org.jetbrains.bazel.flow.open

import com.intellij.configurationStore.ProjectStoreDescriptor
import com.intellij.configurationStore.ProjectStorePathCustomizer
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.diagnostic.getOrHandleException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project.DIRECTORY_STORE_FOLDER
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.dotIdeaDirectoryLocation
import org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.isDefaultProjectViewFile
import org.jetbrains.bazel.languages.projectview.project.ProjectViewFileLocalizer.pickProjectViewFileForProject
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.utils.refreshAndFindVirtualFile
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

  @RequiresReadLockAbsence(generateAssertion = false)
  private fun selectDotIdeaPath(projectRootPath: Path, projectViewPath: Path): Path {
    // The VFS refresh requires that there be no active read locks;
    // otherwise, it will not perform any action and will not provide a warning.
    ThreadingAssertions.assertNoReadAccess()
    val projectViewFile = projectViewPath.refreshAndFindVirtualFile() ?: return projectRootPath.defaultDotIdeaDirectory()
    val rootDir = projectRootPath.refreshAndFindVirtualDirectory() ?: return projectRootPath.defaultDotIdeaDirectory()
    return runReadActionBlocking {
      projectViewFile
        .findPsiFile(ProjectManager.getInstance().defaultProject)
        ?.let { it as? ProjectViewPsiFile }
        ?.let { file ->
          // In principle, ProjectView.fromProjectViewPsiFile should not throw.
          // However, in case it does, it breaks the project opening, so it's better to keep it in a try-catch block.
          runCatching { ProjectView.fromProjectViewPsiFile(file, rootDir) }
            .getOrHandleException { log.error("Failed to parse project view file. Falling back to default dotIdea directory.", it) }
        }
        ?.dotIdeaDirectoryLocation
        ?.let(projectRootPath::resolve) ?: projectRootPath.defaultDotIdeaDirectory()
    }
  }

  private fun Path.defaultDotIdeaDirectory(): Path = resolve(DIRECTORY_STORE_FOLDER)

  companion object {
    private val log = logger<BazelProjectStorePathCustomizer>()
  }
}
