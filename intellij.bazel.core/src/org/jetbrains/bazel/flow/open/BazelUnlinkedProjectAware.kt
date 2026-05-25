package org.jetbrains.bazel.flow.open

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.autolink.ExternalSystemProjectLinkListener
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.coroutines.BazelApplicationCoroutineScopeService

@ApiStatus.Internal
class BazelUnlinkedProjectAware : ExternalSystemUnlinkedProjectAware {
  companion object {
    private val log = logger<BazelUnlinkedProjectAware>()
  }

  override val systemId: ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  override fun isBuildFile(project: Project, buildFile: VirtualFile): Boolean =
    buildFile.isBazelWorkspaceFile()

  override fun isLinkedProject(project: Project, externalProjectPath: String): Boolean =
    project.isBazelProject

  override suspend fun linkAndLoadProjectAsync(project: Project, externalProjectPath: String) {
    BazelApplicationCoroutineScopeService.getInstance().launch {
      val file = readAction {
        LocalFileSystem.getInstance()
          .findFileByPath(externalProjectPath)
          ?.children
          ?.firstOrNull { isBuildFile(project, it) }
      }?.toNioPathOrNull()

      if (file != null) {
        closeAndReopenAsBazelProject(project, file)
      } else {
        log.warn("Unable to find Bazel project file for $externalProjectPath")
        return@launch
      }
    }
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
