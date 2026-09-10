package org.jetbrains.bazel.sync

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
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
import kotlin.io.path.relativeToOrNull

internal class SyncWorkspaceUpdater(private val project: Project) {
  suspend fun update(requested: ProjectSyncScope, previous: WorkspaceSnapshot, context: SyncWorkspaceContext): SyncWorkspaceUpdate {
    val scope = effectiveScope(context, previous, requested)
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
        if (scope.patterns.isEmpty()) {
          return SyncWorkspaceUpdate(scope = scope, status = SyncWorkspaceStatus.SUCCESS, snapshot = previous)
        }
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

      is ProjectSyncScope.Files -> throw IllegalStateException("Files sync scope should already have been promoted by effectiveScope(...)")
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

  private fun SyncFile.isFullResyncRequired(): Boolean =
    this.kind is SyncFileKind.Module ||
    this.kind in setOf(SyncFileKind.Workspace, SyncFileKind.ProjectView, SyncFileKind.BazelDotFile) ||
    (this.kind is SyncFileKind.Starlark && this.kind.label == null)

  private suspend fun effectiveScope(
    context: SyncWorkspaceContext,
    previous: WorkspaceSnapshot,
    requested: ProjectSyncScope,
  ): ProjectSyncScope =
    when (requested) {
      is ProjectSyncScope.Files -> {
        // here we're using stale repo mapping from previous sync, however it's ok
        // modifying repos cause full resync anyway
        // the issue appears before first sync, where repo mapping is always empty
        // correct approach here would be to promote to full sync always when snapshot is empty
        val classifier = SyncFileClassifier(repoMapping = previous.repoMapping, bazelInfo = context.server.bazelInfo)
        val syncFiles = requested.files.map { classifier.classify(it) }
        if (syncFiles.any { it.isFullResyncRequired() }) {
          return ProjectSyncScope.Full(build = requested.build, phased = false)
        }

        val patternsToResync = findAffectedPatterns(context, syncFiles, previous.repoMapping)
        if (patternsToResync.isEmpty()) {
          project.syncConsole.addDiagnosticMessage(
            taskId = context.taskId,
            message = BazelBackendBundle.message("progress.text.no.targets.found.for.requested.files.to.sync"),
            severity = MessageEvent.Kind.WARNING,
          )
        }

        // promote to normal partial sync
        ProjectSyncScope.Targets(patterns = patternsToResync.toList(), build = requested.build)
      }

      else -> requested
    }

  private suspend fun findAffectedPatterns(
    context: SyncWorkspaceContext,
    syncFiles: List<SyncFile>,
    repoMapping: RepoMapping,
  ): Set<Label> {
    val patternsToResync = syncFiles.asSequence()
      .flatMap { syncFile ->
        when (val kind = syncFile.kind) {
          is SyncFileKind.Build -> sequenceOf(kind.pattern)
          is SyncFileKind.Folder -> sequenceOf(kind.pattern)
          else -> sequenceOf()
        }
      }
      .toMutableSet()

    patternsToResync += findAffectedPatternsBySourceFiles(syncFiles, context)
    patternsToResync += findAffectedPatternsByStarlarkFiles(context, syncFiles, repoMapping)

    return patternsToResync
  }

  private suspend fun findAffectedPatternsByStarlarkFiles(
    context: SyncWorkspaceContext,
    syncFiles: List<SyncFile>,
    repoMapping: RepoMapping,
  ): List<Label> {
    val workspaceRoot = context.server.bazelInfo.workspaceRoot
    val starlarkFiles = syncFiles.filter { it.kind is SyncFileKind.Starlark && it.path.startsWith(workspaceRoot) }

    // for changed starlark files, try to find affected BUILD files
    return if (starlarkFiles.isNotEmpty()) {
      project.syncConsole.withSubtask(
        subtaskId = context.taskId.subTask("query_buildfiles"),
        message = BazelBackendBundle.message("progress.title.querying.build.files"),
      ) { taskId ->
        BuildfilesQuery.findDependantBuildFiles(
          server = context.server,
          workspaceRelativePaths = starlarkFiles.mapNotNull { it.path.relativeToOrNull(workspaceRoot) }.toSet(),
          universeRepos = starlarkFiles.mapNotNull { ((it.kind as SyncFileKind.Starlark).label as? ResolvedLabel)?.repo }.toSet(),
          repoMapping = repoMapping,
          taskId = taskId,
        )
      }
    }
    else {
      listOf()
    }
  }

  private suspend fun findAffectedPatternsBySourceFiles(syncFiles: List<SyncFile>, context: SyncWorkspaceContext): List<Label> {
    val fileLabels = syncFiles.asSequence()
      .map { it.kind }
      .filterIsInstance<SyncFileKind.Source>()
      .map { it.label }
      .toSet()

    // for changed source files, invoke file to target query
    return if (fileLabels.isNotEmpty()) {
      val sources = project.syncConsole.withSubtask(
        subtaskId = context.taskId.subTask("query_source_to_target"),
        message = BazelBackendBundle.message("progress.title.querying.targets.for.sources"),
      ) { taskId ->
        val sources = FileToTargetQuery.findDependantTargetsFromFiles(server = context.server, fileLabels = fileLabels, taskId = taskId)
        for ((fileLabel, targets) in sources) {
          if (targets.isEmpty()) {
            project.syncConsole.addDiagnosticMessage(
              taskId = taskId,
              message = BazelBackendBundle.message("progress.text.file.query.found.no.targets.for", fileLabel),
              severity = MessageEvent.Kind.WARNING,
            )
          }
        }
        sources
      }
      sources.values.flatten()
    }
    else {
      listOf()
    }
  }
}

internal data class SyncWorkspaceUpdate(
  val scope: ProjectSyncScope,
  val status: SyncWorkspaceStatus,
  val snapshot: WorkspaceSnapshot,
)
