package org.jetbrains.bazel.android

import com.android.tools.idea.projectsystem.PROJECT_SYSTEM_SYNC_TOPIC
import com.android.tools.idea.projectsystem.ProjectSystemSyncManager
import com.android.tools.idea.startup.ClearResourceCacheAfterFirstBuild
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project

public class BazelProjectSystemSyncManager(private val project: Project) : ProjectSystemSyncManager {
  init {
    initialNotifySyncEnded(project)
  }

  /**
   * Called by [BazelAndroidModelUpdater] after it finishes
   */
  fun notifySyncEnded(project: Project) {
    DumbService.getInstance(project).smartInvokeLater(
      {
        project.messageBus
          .syncPublisher(PROJECT_SYSTEM_SYNC_TOPIC)
          .syncEnded(ProjectSystemSyncManager.SyncResult.SUCCESS)
      },
      ModalityState.nonModal(),
    )
  }

  private fun initialNotifySyncEnded(project: Project) {
    notifySyncEnded(project)

    // Hack: Android UI preview waits for ClearResourceCacheAfterFirstBuild to have received a successful sync message.
    // But unfortunately, we have a race condition between us sending the initial syncEnded message
    // and ClearResourceCacheAfterFirstBuild starting to listen to PROJECT_SYSTEM_SYNC_TOPIC.
    // Therefore, we have to notify ClearResourceCacheAfterFirstBuild directly.
    DumbService.getInstance(project).smartInvokeLater(
      {
        ClearResourceCacheAfterFirstBuild.getInstance(project).syncSucceeded()
      },
      ModalityState.nonModal(),
    )
  }

  override fun syncProject(reason: ProjectSystemSyncManager.SyncReason): ListenableFuture<ProjectSystemSyncManager.SyncResult> {
    notifySyncEnded(project)
    return Futures.immediateFuture(ProjectSystemSyncManager.SyncResult.SUCCESS)
  }

  override fun isSyncInProgress(): Boolean = false

  override fun isSyncNeeded(): Boolean = false

  override fun getLastSyncResult(): ProjectSystemSyncManager.SyncResult = ProjectSystemSyncManager.SyncResult.SUCCESS
}
