package org.jetbrains.bazel.flow.open

import com.intellij.configurationStore.ProjectStoreDescriptor
import com.intellij.configurationStore.ProjectStorePathCustomizer
import com.intellij.openapi.application.runReadActionBlocking
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.openapi.vfs.refreshAndFindVirtualDirectory
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.dotIdeaDirectoryLocation
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.utils.refreshAndFindVirtualFile
import java.nio.file.Path

private class BazelProjectStorePathCustomizer : ProjectStorePathCustomizer {
  override fun getStoreDirectoryPath(projectRoot: Path): ProjectStoreDescriptor? {
    // do not use isDirectory, as we also want to check that the file exists
    if (!projectRoot.hasNameOf(*Constants.SUPPORTED_CONFIG_FILE_NAMES) &&
      !projectRoot.hasExtensionOf(*Constants.SUPPORTED_EXTENSIONS)
    ) return null
    val historicalProjectBasePath = projectRoot.refreshAndFindVirtualFile()
      ?.let(::findProjectFolderFromVFile)
      ?.toNioPath() ?: projectRoot.parent
    val dotIdea = runReadActionBlocking { historicalProjectBasePath.selectDotIdeaPath() }
    return BazelProjectStoreDescriptor(
      projectIdentityFile = projectRoot,
      dotIdea = dotIdea,
      historicalProjectBasePath = historicalProjectBasePath,
      workspaceXml = dotIdea.resolve("workspace.xml"),
    )
  }

  private fun Path.selectDotIdeaPath(): Path = this
    .refreshAndFindVirtualDirectory()
    ?.let { ProjectViewFileUtils.calculateProjectViewFilePath(projectRootDir = it, projectViewPath = null) }
    ?.refreshAndFindVirtualFile()
    ?.findPsiFile(ProjectManager.getInstance().defaultProject)
    ?.let { it as? ProjectViewPsiFile }
    ?.let(ProjectView::fromProjectViewPsiFile)
    ?.dotIdeaDirectoryLocation
    ?.let(this::resolve) ?: this.resolve(".idea")
}
