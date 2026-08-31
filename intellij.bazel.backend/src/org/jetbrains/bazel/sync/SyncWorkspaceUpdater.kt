package org.jetbrains.bazel.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.sync.task.SyncPhase
import org.jetbrains.bazel.sync.task.SyncWorkspaceContext
import org.jetbrains.bazel.sync.task.SyncWorkspaceStatus
import org.jetbrains.bazel.sync.workspace.mapper.BazelWorkspaceResolver
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.toIncompleteSnapshot
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector

internal class SyncWorkspaceUpdater(private val project: Project) {
  suspend fun update(requested: ProjectSyncScope, previous: WorkspaceSnapshot, context: SyncWorkspaceContext): SyncWorkspaceUpdate {
    val scope = effectiveScope(context, requested)
    return when (scope) {
      is ProjectSyncScope.Full -> {
        val resolved = resolveWorkspace(context, WorkspaceBuildTargetSelector.AllTargets)
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

      is ProjectSyncScope.Targets -> {
        // TODO: right now `resolveWorkspace` is dumb, bazel build will be called for small subset of targets
        //  but BEP still return full transitive closure of all output artifacts. That means re still have to read
        //  large number of .textproto files despite most of them begin unchanged.
        //  Solution here is to track output artifact identity (modification time, content length for local artifacts)
        //  and compare those against previous state. As a result we get "artifact diff" and we invoke textproto read + parse
        //  only on changed files.
        val resolved = resolveWorkspace(context, WorkspaceBuildTargetSelector.SpecificTargets(scope.patterns))
        val incomplete = WorkspaceSnapshotBuilder.buildIncomplete(resolved = resolved)
        val snapshot = project.syncConsole.withSubtask(
          subtaskId = context.taskId.subTask("workspace_snapshot_merge"),
          message = BazelBackendBundle.message("console.task.sync.snapshot.updating")
        ) {
          WorkspaceSnapshotBuilder.merge(
            project = project,
            projectView = context.server.projectView,
            snapshots = listOf(previous.toIncompleteSnapshot(), incomplete),
          )
        }
        SyncWorkspaceUpdate(
          scope = scope,
          status = if (resolved.hasError) SyncWorkspaceStatus.PARTIAL else SyncWorkspaceStatus.SUCCESS,
          snapshot = snapshot,
        )
      }

      is ProjectSyncScope.Files -> throw UnsupportedOperationException("not supported yet")
    }
  }

  private suspend fun resolveWorkspace(context: SyncWorkspaceContext, selector: WorkspaceBuildTargetSelector) =
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
        selector = selector,
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
