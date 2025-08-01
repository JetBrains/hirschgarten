package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.EarlyBazelSyncProject
import org.jetbrains.bazel.ui.console.ids.PROJECT_SYNC_TASK_ID

interface BazelWorkspaceResolver {

  suspend fun getOrFetchResolvedWorkspace(
    scope: ProjectSyncScope = SecondPhaseSync,
    taskId: String = PROJECT_SYNC_TASK_ID,
    force: Boolean = false,
  ): BazelResolvedWorkspace

  suspend fun getOrFetchMappedProject(
    scope: ProjectSyncScope = SecondPhaseSync,
    taskId: String = PROJECT_SYNC_TASK_ID,
    force: Boolean = false,
  ): BazelMappedProject

  suspend fun getOrFetchSyncedProject(
    build: Boolean = false,
    taskId: String = PROJECT_SYNC_TASK_ID,
    force: Boolean = false,
  ): EarlyBazelSyncProject

  suspend fun <T> withEndpointProxy(func: suspend (BazelEndpointProxy) -> T): T

}
