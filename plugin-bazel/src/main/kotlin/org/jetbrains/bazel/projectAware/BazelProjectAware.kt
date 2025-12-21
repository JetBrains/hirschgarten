package org.jetbrains.bazel.projectAware

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectId
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectListener
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectReloadContext
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemRefreshStatus
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.coroutines.BazelCoroutineService
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bazel.sync_new.flow.SyncBridgeService
import org.jetbrains.bazel.sync_new.flow.SyncScope
import org.jetbrains.bazel.sync_new.isNewSyncEnabled

abstract class BazelProjectAware(private val workspace: BazelWorkspace) : ExternalSystemProjectAware {
  override val settingsFiles: Set<String>
    get() = emptySet()

  override fun reloadProject(context: ExternalSystemProjectReloadContext) {
    if (context.isExplicitReload) {
      BazelCoroutineService.getInstance(workspace.project).start {
        val project = workspace.project
        if (project.isNewSyncEnabled) {
          project.service<SyncBridgeService>()
            .sync(scope = SyncScope.Incremental())
        } else {
          ProjectSyncTask(project).sync(syncScope = SecondPhaseSync, buildProject = false)
        }
      }
    }
  }

  override fun subscribe(listener: ExternalSystemProjectListener, parentDisposable: Disposable) {
    workspace.project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {
          listener.onProjectReloadStart()
        }

        override fun syncFinished(canceled: Boolean) {
          ProjectViewUtil.expandTopLevel(workspace.project)
          listener.onProjectReloadFinish(
            if (canceled) {
              ExternalSystemRefreshStatus.CANCEL
            } else {
              ExternalSystemRefreshStatus.SUCCESS
            },
          )
        }
      },
    )
  }

  companion object {
    @JvmStatic
    fun initialize(workspace: BazelWorkspace) {
      val projectAwareExtension = ProjectAwareExtension.ep.extensionList.firstOrNull()
      projectAwareExtension?.also {
        val project = workspace.project
        val projectAware =
          object : BazelProjectAware(workspace) {
            override val projectId: ExternalSystemProjectId
              get() = it.getProjectId(project.rootDir)
          }
        val projectTracker = ExternalSystemProjectTracker.getInstance(project)
        projectTracker.register(projectAware)
        projectTracker.activate(projectAware.projectId)
      }
    }

    @JvmStatic
    fun notify(project: Project) {
      val projectAwareExtension = ProjectAwareExtension.ep.extensionList.firstOrNull()
      projectAwareExtension?.also {
        val projectTracker = ExternalSystemProjectTracker.getInstance(project)
        projectTracker.markDirty(it.getProjectId(project.rootDir))
        projectTracker.scheduleChangeProcessing()
      }
    }
  }
}
