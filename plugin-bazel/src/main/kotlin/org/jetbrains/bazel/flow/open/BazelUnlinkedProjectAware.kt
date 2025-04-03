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
import org.jetbrains.bazel.config.isBrokenBspProject

internal class BazelUnlinkedProjectAware : ExternalSystemUnlinkedProjectAware {
  override val systemId: ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  override fun isBuildFile(project: Project, buildFile: VirtualFile): Boolean = BazelOpenProjectProvider().isProjectFile(buildFile)

  override fun isLinkedProject(project: Project, externalProjectPath: String): Boolean =
    project.isBazelProject ||
      // See https://youtrack.jetbrains.com/issue/BAZEL-1500. Broken projects are handled by OpenBrokenBazelProjectStartupActivity already.
      project.isBrokenBspProject

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

  override fun subscribe(
    project: Project,
    listener: ExternalSystemProjectLinkListener,
    parentDisposable: Disposable,
  ) {
  }
}
