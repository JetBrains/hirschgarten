package org.jetbrains.bazel.sync.workspace

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.ui.console.ids.PROJECT_SYNC_TASK_ID
import org.jetbrains.bsp.protocol.BazelProject

interface BazelWorkspaceResolveService {
  suspend fun invalidateCachedState()

  suspend fun getOrFetchResolvedWorkspace(
    scope: ProjectSyncScope = SecondPhaseSync,
    taskId: String = PROJECT_SYNC_TASK_ID,
  ): BazelResolvedWorkspace

  suspend fun getOrFetchSyncedProject(build: Boolean = false, taskId: String = PROJECT_SYNC_TASK_ID): BazelProject

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelWorkspaceResolveService = project.getService(BazelWorkspaceResolveService::class.java)
  }
}
