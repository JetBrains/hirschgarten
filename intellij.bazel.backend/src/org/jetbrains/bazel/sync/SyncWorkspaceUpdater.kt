package org.jetbrains.bazel.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.task.SyncPhase
import org.jetbrains.bazel.sync.task.SyncWorkspaceContext
import org.jetbrains.bazel.sync.task.SyncWorkspaceStatus
import org.jetbrains.bazel.sync.workspace.mapper.BazelWorkspaceResolver
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotBuilder

internal class SyncWorkspaceUpdater(private val project: Project) {
  suspend fun update(requested: ProjectSyncScope, previous: WorkspaceSnapshot, context: SyncWorkspaceContext): SyncWorkspaceUpdate {
    val scope = effectiveScope(context, requested)
    return when (scope) {
      is ProjectSyncScope.Full -> {
        val resolved = resolveWorkspace(context)
        // resolve which produced nothing cannot overwrite what is already there
        if (resolved.hasError && resolved.targets.isEmpty()) {
          return SyncWorkspaceUpdate(scope = scope, status = SyncWorkspaceStatus.FATAL, snapshot = previous)
        }
        val snapshot = WorkspaceSnapshotBuilder.build(
          project = project,
          projectView = context.server.projectView,
          repoMapping = resolved.repoMapping,
          resolved = resolved,
        )
        SyncWorkspaceUpdate(
          scope = scope,
          status = if (resolved.hasError) SyncWorkspaceStatus.PARTIAL else SyncWorkspaceStatus.SUCCESS,
          snapshot = snapshot,
        )
      }

      is ProjectSyncScope.Targets -> throw UnsupportedOperationException("not supported yet")

      is ProjectSyncScope.Files -> throw UnsupportedOperationException("not supported yet")
    }
  }

  private suspend fun resolveWorkspace(context: SyncWorkspaceContext) =
    when (context.phase) {
      SyncPhase.FIRST -> BazelWorkspaceResolver.fetchPhasedWorkspace(
        project = project,
        taskId = context.taskId,
      )

      SyncPhase.SECOND -> BazelWorkspaceResolver.fetchAspectWorkspace(
        project = project,
        allKnownTargets = context.allKnownTargets,
        build = context.buildProject,
        taskId = context.taskId,
      )
    }

  // TODO: here `ProjectSyncScope.Files` gets promoted to `ProjectSyncScope.Targets` or `ProjectSyncScope.Full`
  private fun effectiveScope(context: SyncWorkspaceContext, requested: ProjectSyncScope): ProjectSyncScope = requested
}

internal data class SyncWorkspaceUpdate(
  val scope: ProjectSyncScope,
  val status: SyncWorkspaceStatus,
  val snapshot: WorkspaceSnapshot,
)
