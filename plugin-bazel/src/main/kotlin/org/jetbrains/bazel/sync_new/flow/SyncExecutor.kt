package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.ThreadingAssertions
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.flow.diff.TargetDiffService
import org.jetbrains.bazel.sync_new.flow.diff.TargetPattern
import org.jetbrains.bazel.sync_new.flow.diff.query.QueryTargetHashContributor
import org.jetbrains.bazel.sync_new.graph.EMPTY_ID
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetGraph
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import org.jetbrains.bazel.ui.console.ids.PROJECT_SYNC_TASK_ID
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.RawAspectTarget

class SyncExecutor(
  val project: Project
) {

  private val logger = logger<SyncExecutor>()

  suspend fun execute(scope: SyncScope) : SyncStatus {
    // compute changed targets
    // check for repo mapping changes
    // update graph with new targets
    // run index updating
    // run sync pipelines

    ThreadingAssertions.assertBackgroundThread()

    val syncStore = project.service<SyncStoreService>()
    val ctx = SyncContext(
      project = project,
      scope = scope,
      graph = syncStore.targetGraph,
      syncExecutor = this,
    )

    if (scope is SyncScope.Full) {
      syncStore.syncMetadata.set(SyncMetadata())
      ctx.graph.clear()
      // TODO: clear all indexes here
    }

    val diff = withTask("target_diff", "Computing target diff") {
      computeSyncDiff(scope, ctx.graph)
    }

    withTask("repo_mapping", "Updating repo mapping") {
      updateRepoMapping(diff)
    }

    withTask("target_graph", "Updating target graph") {
      updateTargetGraph(ctx, diff)
    }

    return SyncStatus.Success
  }

  private suspend fun computeSyncDiff(scope: SyncScope, graph: BazelTargetGraph): SyncDiff {
    val diffService = project.serviceAsync<TargetDiffService>()
    if (scope is SyncScope.Full) {
      diffService.clear()
    }

    // TODO: different hash contributor strategies
    val hashContributor = QueryTargetHashContributor()

    return when (scope) {
      is SyncScope.Full -> {
        diffService.computeFreshDiff(hashContributor, graph)
      }

      SyncScope.Incremental -> {
        diffService.computeIncrementalDiff(hashContributor, graph)
      }

      is SyncScope.Partial -> {
        val patterns = scope.targets.map { TargetPattern.Include(it) }
        diffService.computeIncrementalDiff(hashContributor, patterns, graph)
      }
    }
  }

  private suspend fun updateRepoMapping(diff: SyncDiff) {
    val syncStore = project.service<SyncStoreService>()

    var needUpdate = false
    for (target in diff.added) {
      val resolvedLabel = target.label as? ResolvedLabel ?: continue
      val apparentRepo = resolvedLabel.repo as? Apparent ?: continue
      val repoMapping = syncStore.repoMapping
      when (repoMapping) {
        is BzlmodSyncRepoMapping -> {
          if (apparentRepo.repoName !in repoMapping.apparentToCanonical) {
            needUpdate = true
            break
          }
        }

        DisabledSyncRepoMapping -> {
          needUpdate = true
          break
        }
      }
    }

    if (needUpdate) {
      val newRepoMapping = LegacyBazelFrontendBridge.fetchRepoMapping(project)
      syncStore.syncMetadata.modify { it.copy(repoMapping = newRepoMapping) }
    }
  }

  private suspend fun updateTargetGraph(ctx: SyncContext, diff: SyncDiff) {
    val graph = ctx.graph

    // remove targets
    for (target in diff.removed) {
      val id = graph.getVertexIdByLabel(target.label)
      if (id != EMPTY_ID) {
        graph.removeVertexById(id)
      }
    }

    val targetsToFetch = (diff.added + diff.changed)
      .map { it.label }
      .toList()
    val rawTargets = fetchRawAspects(targetsToFetch)
      .filter { it.target.label() in targetsToFetch }
    val builder = SyncTargetBuilder(
      project = project,
      pathsResolver = LegacyBazelFrontendBridge.fetchBazelPathsResolver(project),
    )
    val toRecompute = mutableListOf<BazelTargetVertex>()
    for (target in rawTargets) {
      val existingVertexId = graph.getVertexIdByLabel(label = target.target.label())

      // if target is already in the graph, remove it first
      if (existingVertexId != EMPTY_ID) {
        graph.removeVertexById(existingVertexId)
      }

      // add target
      val vertex = builder.buildTargetVertex(ctx, target)
      graph.addVertex(vertex)
      toRecompute.add(vertex)
    }

    for (vertex in toRecompute) {
      for (directDependency in vertex.genericData.directDependencies) {
        val toVertexId = graph.getVertexIdByLabel(directDependency.label)
        if (toVertexId == EMPTY_ID) {
          logger.warn("Missing dependency for ${vertex.label}: ${directDependency.label}")
          continue
        }
        val edge = builder.buildTargetEdge(ctx, vertex.vertexId, toVertexId)
        graph.addEdge(edge)
      }
    }
  }

  private suspend fun fetchRawAspects(targets: List<Label>): List<RawAspectTarget> {
    if (targets.isEmpty()) {
      return listOf()
    }
    return withTask("fetch_raw_aspects", "Fetching aspect targets") {
      val syncStore = project.service<SyncStoreService>()
      val repoMapping = LegacyBazelFrontendBridge.toLegacyRepoMapping(syncStore.repoMapping)
      LegacyBazelFrontendBridge.fetchRawAspectTargets(project = project, repoMapping = repoMapping, targets = targets)
    }
  }

  private suspend fun <T> withTask(taskId: String, message: String, action: suspend () -> T): T {
    return project.syncConsole.withSubtask(
      taskId = PROJECT_SYNC_TASK_ID,
      subtaskId = taskId,
      message = message,
      block = { action() },
    )
  }

}
