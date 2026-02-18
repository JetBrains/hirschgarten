package org.jetbrains.bazel.sync.workspace

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bsp.protocol.BazelProject
import org.jetbrains.bsp.protocol.TaskId

interface BazelWorkspaceResolveService {
  suspend fun invalidateCachedState()

  suspend fun getOrFetchResolvedWorkspace(
    scope: ProjectSyncScope = SecondPhaseSync,
    taskId: TaskId,
  ): BazelResolvedWorkspace

  suspend fun getOrFetchSyncedProject(
    build: Boolean = false,
    taskId: TaskId
  ): BazelProject

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelWorkspaceResolveService = project.getService(BazelWorkspaceResolveService::class.java)
  }
}
