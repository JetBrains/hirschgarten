package org.jetbrains.bazel.flow.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.externalSystem.autolink.ExternalSystemProjectLinkListener
import com.intellij.openapi.externalSystem.autolink.ExternalSystemUnlinkedProjectAware
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.isBrokenBazelProject
import java.nio.file.Path

class BazelUnlinkedProjectAware : ExternalSystemUnlinkedProjectAware {
  companion object {
    // See https://youtrack.jetbrains.com/issue/BAZEL-1500. Broken projects are handled by OpenBrokenBazelProjectStartupActivity already.
    val Project.isLinkedBazelProject: Boolean
      get() = isBazelProject || isBrokenBazelProject

    /**
     * Closes the current project and reopens it as a Bazel project.
     * Must be called from application-level scope (use [BazelApplicationCoroutineScopeService.launch]).
     */
    internal suspend fun closeAndReopenAsBazelProject(project: Project, file: Path) {
      val projectManager = serviceAsync<ProjectManager>()
      withContext(Dispatchers.EDT) {
        projectManager.closeAndDispose(project)
      }

      ProjectUtil.openOrImportAsync(
        file = file,
        options = OpenProjectTask {
          runConfigurators = true
          isNewProject = true
          useDefaultProjectAsTemplate = true
          forceOpenInNewFrame = true
        },
      )
    }
  }

  override val systemId: ProjectSystemId = BazelPluginConstants.SYSTEM_ID

  override fun isBuildFile(project: Project, buildFile: VirtualFile): Boolean =
    BazelOpenProjectProvider().isProjectFile(buildFile)

  override fun isLinkedProject(project: Project, externalProjectPath: String): Boolean =
    project.isLinkedBazelProject

  override suspend fun linkAndLoadProjectAsync(project: Project, externalProjectPath: String) {
    val service = serviceAsync<BazelApplicationCoroutineScopeService>()
    service.launch {
      val file = readAction {
        LocalFileSystem.getInstance()
          .findFileByPath(externalProjectPath)
          ?.children
          ?.firstOrNull { isBuildFile(project, it) }
      }?.toNioPath()!!

      closeAndReopenAsBazelProject(project, file)
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

/**
 * Application-level coroutine scope service for operations that must survive project closure.
 * Use this when you need to close a project and then open another one.
 */
@Service(Service.Level.APP)
internal class BazelApplicationCoroutineScopeService(private val coroutineScope: CoroutineScope) {
  fun launch(block: suspend CoroutineScope.() -> Unit): Job =
    coroutineScope.launch(
      start = CoroutineStart.UNDISPATCHED,
      block = block,
    )
}
