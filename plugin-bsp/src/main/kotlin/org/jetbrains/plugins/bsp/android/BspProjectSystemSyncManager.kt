package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.project.Project
import com.intellij.ui.AppUIUtil

public class BspProjectSystemSyncManager(private val project: Project) : ProjectSystemSyncManager {
  override fun getLastSyncResult(): ProjectSystemSyncManager.SyncResult = ProjectSystemSyncManager.SyncResult.SUCCESS

  override fun isSyncInProgress(): Boolean = false

  override fun isSyncNeeded(): Boolean = false

  override fun syncProject(
    reason: ProjectSystemSyncManager.SyncReason,
  ): ListenableFuture<ProjectSystemSyncManager.SyncResult> {
    AppUIUtil.invokeLaterIfProjectAlive(project) {
      onSyncEnded(project)
    }
    return Futures.immediateFuture(ProjectSystemSyncManager.SyncResult.SUCCESS)
  }

  public companion object {
    public fun onSyncEnded(project: Project) {
      project.messageBus.syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC).syncEnded(ProjectSystemSyncManager.SyncResult.SUCCESS)
    }
  }
}
