package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult

class BazelWorkspaceResolveServiceMock(
  private val resolvedWorkspace: BazelResolvedWorkspace? = null,
  private val bazelProject: WorkspaceBuildTargetsResult? = null,
) : BazelWorkspaceResolveService {
  override suspend fun getOrFetchResolvedWorkspace(scope: ProjectSyncScope, taskId: TaskId): BazelResolvedWorkspace =
    resolvedWorkspace ?: error("resolved workspace is not set")

  override suspend fun getOrFetchSyncedProject(build: Boolean, taskId: TaskId): WorkspaceBuildTargetsResult =
    bazelProject ?: error("early bazel sync project is not set")

  override suspend fun invalidateCachedState() {
    // no-op
  }
}
