package org.jetbrains.plugins.bsp.android

import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.services.MagicMetaModelService

public class BspProjectSystemSyncManager(private val project: Project) : ProjectSystemSyncManager {
  init {
    val magicMetaModel = MagicMetaModelService.getInstance(project).value
    magicMetaModel.registerTargetLoadListener {
      onSyncEnded(project)
    }
    onSyncEnded(project)
  }

  private fun onSyncEnded(project: Project) {
    DumbService.getInstance(project).smartInvokeLater {
      project.messageBus
        .syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC)
        .syncEnded(ProjectSystemSyncManager.SyncResult.SUCCESS)
    }
  }

  override fun syncProject(
    reason: ProjectSystemSyncManager.SyncReason,
  ): ListenableFuture<ProjectSystemSyncManager.SyncResult> {
    onSyncEnded(project)
    return Futures.immediateFuture(ProjectSystemSyncManager.SyncResult.SUCCESS)
  }

  override fun isSyncInProgress(): Boolean = false

  override fun isSyncNeeded(): Boolean = false

  override fun getLastSyncResult(): ProjectSystemSyncManager.SyncResult = ProjectSystemSyncManager.SyncResult.SUCCESS
}
