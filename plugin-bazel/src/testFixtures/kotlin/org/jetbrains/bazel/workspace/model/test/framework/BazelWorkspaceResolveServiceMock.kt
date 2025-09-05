package org.jetbrains.bazel.workspace.model.test.framework

import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
import org.jetbrains.bazel.sync.workspace.EarlyBazelSyncProject

class BazelWorkspaceResolveServiceMock(
  private val resolvedWorkspace: BazelResolvedWorkspace? = null,
  private val earlyBazelSyncProject: EarlyBazelSyncProject? = null,
) : BazelWorkspaceResolveService {
  override suspend fun getOrFetchResolvedWorkspace(scope: ProjectSyncScope, taskId: String): BazelResolvedWorkspace =
    resolvedWorkspace ?: error("resolved workspace is not set")

  override suspend fun getOrFetchSyncedProject(build: Boolean, taskId: String): EarlyBazelSyncProject =
    earlyBazelSyncProject ?: error("early bazel sync project is not set")

  override suspend fun invalidateCachedState() {
    // no-op
  }
}
