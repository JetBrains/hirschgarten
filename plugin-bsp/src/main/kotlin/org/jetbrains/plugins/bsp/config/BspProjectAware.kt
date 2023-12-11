package org.jetbrains.plugins.bsp.config

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.bsp.server.tasks.SyncProjectTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService

public interface BspProjectAwareExtension {
  public fun getProjectId(projectPath: VirtualFile): ExternalSystemProjectId
  public val eligibleConfigFileNames: List<String>
  public val eligibleConfigFileExtensions: List<String>

  public companion object {
    public val ep: ExtensionPointName<BspProjectAwareExtension> =
      ExtensionPointName.create("org.jetbrains.bsp.bspProjectAwareExtension")
  }
}

public abstract class BspProjectAware(private val workspace: BspWorkspace) :
  ExternalSystemProjectAware {
  override val settingsFiles: Set<String>
    get() = emptySet()

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    if (context.isExplicitReload) {
      BspCoroutineService.getInstance(workspace.project).start {
        SyncProjectTask(workspace.project).execute(
          shouldRunInitialSync = true,
          shouldBuildProject = false,
          shouldRunResync = false,
        )
      }
    }
  }

  override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
    workspace.project.messageBus.connect().subscribe(BspWorkspaceListener.TOPIC, object : BspWorkspaceListener {
      override fun syncStarted() {
        listener.onProjectReloadStart()
      }

      override fun syncFinished(canceled: Boolean) {
        listener.onProjectReloadFinish(
          if (canceled) ExternalSystemRefreshStatus.CANCEL
          else ExternalSystemRefreshStatus.SUCCESS
        )
      }
    })
  }

  public companion object {
    @JvmStatic
    public fun initialize(workspace: BspWorkspace) {
      val projectAwareExtension = BspProjectAwareExtension.ep.extensionList.firstOrNull()
      projectAwareExtension?.also {
        val project = workspace.project
        val projectAware = object : BspProjectAware(workspace) {
          override val projectId: ExternalSystemProjectId
            get() = it.getProjectId(project.rootDir)
        }
        val projectTracker = ExternalSystemProjectTracker.getInstance(project)
        projectTracker.register(projectAware)
        projectTracker.activate(projectAware.projectId)
      }
    }

    @JvmStatic
    public fun notify(project: Project) {
      val projectAwareExtension = BspProjectAwareExtension.ep.extensionList.firstOrNull()
      projectAwareExtension?.also {
        val projectTracker = ExternalSystemProjectTracker.getInstance(project)
        projectTracker.markDirty(it.getProjectId(project.rootDir))
        projectTracker.scheduleChangeProcessing()
      }
    }
  }
}
