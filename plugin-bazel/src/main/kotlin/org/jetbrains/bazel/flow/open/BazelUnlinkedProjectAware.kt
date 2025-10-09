package org.jetbrains.bazel.flow.open

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autolink.ExternalSystemProjectLinkListener
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getPresentablePath
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.isBrokenBazelProject

internal class BazelUnlinkedProjectAware : ExternalSystemUnlinkedProjectAware {
  override val systemId: ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  override fun isBuildFile(project: Project, buildFile: VirtualFile): Boolean = BazelOpenProjectProvider().isProjectFile(buildFile)

  override fun isLinkedProject(project: Project, externalProjectPath: String): Boolean =
    project.isBazelProject ||
      // See https://youtrack.jetbrains.com/issue/BAZEL-1500. Broken projects are handled by OpenBrokenBazelProjectStartupActivity already.
      project.isBrokenBazelProject

  override suspend fun linkAndLoadProjectAsync(project: Project, externalProjectPath: String) {
    BazelOpenProjectProvider().linkProject(getProjectFile(externalProjectPath), project)
  }

  private fun getProjectFile(projectFilePath: String): VirtualFile {
    val localFileSystem = LocalFileSystem.getInstance()
    val projectFile = localFileSystem.refreshAndFindFileByPath(projectFilePath)
    if (projectFile == null) {
      val shortPath = getPresentablePath(projectFilePath)
      throw IllegalArgumentException(ExternalSystemBundle.message("error.project.does.not.exist", systemId.readableName, shortPath))
    }
    return projectFile
  }

  override suspend fun unlinkProject(project: Project, externalProjectPath: String) {
    // This method must be implemented as it is a part of an API.
    // If was mandatory from the very beginning, this assertion sat in a parent.
    TODO("Not yet implemented")
  }

  override fun subscribe(
    project: Project,
    listener: ExternalSystemProjectLinkListener,
    parentDisposable: Disposable,
  ) {
  }
}
