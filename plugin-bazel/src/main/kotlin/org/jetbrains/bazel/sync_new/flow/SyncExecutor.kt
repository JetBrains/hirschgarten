package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.ThreadingAssertions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.flow.hash_diff._old.TargetDiffService
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseTargetPattern
import org.jetbrains.bazel.sync_new.flow.hash_diff._old.query.QueryTargetHashContributor
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSService
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseService
import org.jetbrains.bazel.sync_new.flow.universe.syncRepoMapping
import org.jetbrains.bazel.sync_new.flow.universe_expand.SyncExpandService
import org.jetbrains.bazel.sync_new.graph.EMPTY_ID
import org.jetbrains.bazel.sync_new.graph.impl.BazelFastTargetGraph
import org.jetbrains.bazel.sync_new.index.SyncIndexService
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdaterProvider
import org.jetbrains.bazel.sync_new.lang.SyncLanguageService
import org.jetbrains.bazel.sync_new.storage.BazelStorageService
import org.jetbrains.bazel.sync_new.storage.LifecycleStoreContext
import org.jetbrains.bsp.protocol.RawAspectTarget

class SyncExecutor(
  val project: Project,
) {

  private val logger = logger<SyncExecutor>()

  suspend fun execute(scope: SyncScope): SyncStatus {
    // compute changed targets
    // check for repo mapping changes
    // update graph with new targets
    // run index updating
    // run sync pipelines
    ThreadingAssertions.assertBackgroundThread()

    val universeDiff = project.service<SyncUniverseService>()
      .computeUniverseDiff(scope)

    val vfsDiff = project.service<SyncVFSService>()
      .computeColdDiff(scope, universeDiff)
    //
    //val connector = project.service<BazelConnectorService>()
    //  .ofLegacyTask()
    //val result = connector.query {
    //  defaults()
    //  keepGoing()
    //  output(QueryOutput.PROTO)
    //  query("deps(${(vfsDiff.changed + vfsDiff.added).joinToString(separator = " + ") { it.toString() }})")
    //}

    val expandedDiff = project.service<SyncExpandService>()
      .expandDependencyDiff(vfsDiff)

    println(universeDiff)
    println(vfsDiff)
    println(expandedDiff)

    return SyncStatus.Success

    val syncStore = withTask(project, "sync_store_init", "Initializing sync store") {
      val store = project.service<SyncStoreService>()
      if (scope is SyncScope.Full) {
        store.syncMetadata.set(SyncMetadata())
        store.targetGraph.clear()
        project.serviceAsync<SyncIndexService>().invalidateAll()
        project.service<SyncVFSService>().resetAll()
      }
      store
    }
    val ctx = SyncContext(
      project = project,
      scope = scope,
      graph = syncStore.targetGraph,
      syncExecutor = this,
      languageService = service<SyncLanguageService>(),
    )

    val diff = withTask(project, "target_diff", "Computing target diff") {
      computeSyncDiff(scope, ctx.graph)
    }

    //withTask(project, "repo_mapping", "Updating repo mapping") {
    //  updateRepoMapping(ctx, diff)
    //}

    withTask(project, "target_graph", "Updating target graph") {
      updateTargetGraph(ctx, diff)
    }

    withTask(project, "update_internal_indexes", "Updating internal indexes") {
      updateInternalIndexes(ctx, diff)
    }

    withTask(project, "save_internal_stores", "Saving internal store") {
      (project.service<BazelStorageService>() as? LifecycleStoreContext)?.save(force = true)
    }

    return SyncStatus.Success
  }

  private suspend fun updateInternalIndexes(ctx: SyncContext, diff: SyncDiff) {
    withContext(Dispatchers.IO) {
      SyncIndexUpdaterProvider.ep.extensionList
        .flatMap { it.createUpdaters(ctx) }
        .map { updater ->
          async {
            updater.updateIndexes(ctx, diff)
          }
        }
        .awaitAll()
    }
  }

  private suspend fun SyncConsoleTask.computeSyncDiff(scope: SyncScope, graph: BazelFastTargetGraph): SyncDiff {
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
        val patterns = scope.targets.map { SyncUniverseTargetPattern.Include(it) }
        diffService.computeIncrementalDiff(hashContributor, patterns, graph)
      }
    }
  }

  //private suspend fun updateRepoMapping(ctx: SyncContext, diff: SyncDiff) {
  //  val syncStore = project.service<SyncStoreService>()
  //
  //  var needUpdate = ctx.scope is SyncScope.Full
  //  for (target in diff.added) {
  //    val resolvedLabel = target.label as? ResolvedLabel ?: continue
  //    val apparentRepo = resolvedLabel.repo as? Apparent ?: continue
  //    val repoMapping = syncStore.repoMapping
  //    when (repoMapping) {
  //      is BzlmodSyncRepoMapping -> {
  //        if (apparentRepo.repoName !in repoMapping.apparentToCanonical) {
  //          needUpdate = true
  //          break
  //        }
  //      }
  //
  //      DisabledSyncRepoMapping -> {
  //        needUpdate = true
  //        break
  //      }
  //    }
  //  }
  //
  //  if (needUpdate) {
  //    val newRepoMapping = LegacyBazelFrontendBridge.fetchRepoMapping(project)
  //    syncStore.syncMetadata.modify { it.copy(repoMapping = newRepoMapping) }
  //  }
  //}

  private suspend fun SyncConsoleTask.updateTargetGraph(ctx: SyncContext, diff: SyncDiff) {
    val graph = ctx.graph

    // remove targets
    for (target in diff.removed) {
      val id = graph.getVertexIdByLabel(target.label)
      if (id != EMPTY_ID) {
        graph.removeVertexById(id)
      }
    }

    val builder = SyncTargetBuilder(
      project = project,
      pathsResolver = LegacyBazelFrontendBridge.fetchBazelPathsResolver(project),
    )
    val targetsToFetch = (diff.added.map { it.label } + diff.changed.map { it.label })
      .toList()
    val rawTargets = fetchRawAspects(ctx, targetsToFetch)
    val vertices = withTask("target_build", "Converting targets") {
      withContext(Dispatchers.IO) {
        rawTargets.map {
          async {
            it to builder.buildTargetVertex(ctx, it)
          }
        }.awaitAll()
      }
    }
    withTask("apply_diff", "Applying graph diff") {
      for ((_, vertex) in vertices) {
        val existingVertexId = graph.getVertexIdByLabel(label = vertex.label)

        // if target is already in the graph, remove it first
        if (existingVertexId != EMPTY_ID) {
          graph.removeVertexById(existingVertexId)
        }

        // add target
        vertex.vertexId = graph.getNextVertexId()
        graph.addVertex(vertex)
      }

      for ((raw, vertex) in vertices) {
        for (dependency in raw.target.dependenciesList) {
          val toVertexId = graph.getVertexIdByLabel(dependency.label())
          if (toVertexId == EMPTY_ID) {
            logger.warn("Missing dependency for ${vertex.label}: ${dependency.label()}")
            continue
          }
          val edge = builder.buildTargetEdge(ctx, vertex.vertexId, toVertexId, dependency)
          graph.addEdge(edge)
        }
      }
    }
  }

  private suspend fun SyncConsoleTask.fetchRawAspects(ctx: SyncContext, targets: List<Label>): List<RawAspectTarget> {
    if (targets.isEmpty()) {
      return listOf()
    }
    return withTask("fetch_partial_aspects", "Fetching partial targets") {
      LegacyBazelFrontendBridge.fetchPartialTargets(
        project = project,
        repoMapping = project.syncRepoMapping,
        targets = targets,
      )
    }
  }

}
