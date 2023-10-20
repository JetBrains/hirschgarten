package org.jetbrains.bazel.flow.open

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autolink.ExternalSystemProjectLinkListener
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.plugins.bsp.config.isBspProject

internal class BazelBspUnlinkedProjectAware : ExternalSystemUnlinkedProjectAware {
  override val systemId: ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  override fun isBuildFile(project: Project, buildFile: VirtualFile): Boolean =
    BazelBspOpenProjectProvider().canOpenProject(buildFile)

  override fun isLinkedProject(project: Project, externalProjectPath: String): Boolean =
    project.isBspProject

  override fun linkAndLoadProject(project: Project, externalProjectPath: String) {
    BazelBspOpenProjectProvider().linkToExistingProject(externalProjectPath, project)
  }

  override fun subscribe(project: Project, listener: ExternalSystemProjectLinkListener, parentDisposable: Disposable) {}
}
