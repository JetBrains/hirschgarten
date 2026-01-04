package org.jetbrains.bazel.sync_new.flow

import com.google.common.collect.HashMultimap
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.UnindexedFilesScannerExecutor
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import com.intellij.util.concurrency.ThreadingAssertions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.server.label.label
import org.jetbrains.bazel.sync_new.SyncFlagsService
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.flow.hash_diff.SyncHasherService
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseService
import org.jetbrains.bazel.sync_new.flow.universe.syncRepoMapping
import org.jetbrains.bazel.sync_new.flow.universe_expand.SyncExpandService
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSService
import org.jetbrains.bazel.sync_new.graph.EMPTY_ID
import org.jetbrains.bazel.sync_new.index.SyncIndexService
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdaterProvider
import org.jetbrains.bazel.sync_new.lang.SyncLanguagePlugin
import org.jetbrains.bazel.sync_new.lang.SyncLanguageService
import org.jetbrains.bazel.ui.console.ConsoleService
import org.jetbrains.bsp.protocol.RawAspectTarget

class SyncExecutor(
  val project: Project,
) {

  private val logger = logger<SyncExecutor>()

  suspend fun execute(scope: SyncScope): SyncStatus {
    ThreadingAssertions.assertBackgroundThread()

    val syncStore = withTask(project, "sync_store_init", "Initializing sync store") {
      val store = project.service<SyncStoreService>()
      if (scope is SyncScope.Full) {
        store.syncMetadata.reset()
        store.targetGraph.clear()
        project.serviceAsync<SyncIndexService>().invalidateAll()
        project.service<SyncVFSService>().resetAll()
      }
      store
    }
    withTask(project, "warmup_server", "Warming up server") {
      // force server to start
      project.connection.runWithServer { /* noop */ }
    }
    val ctx = SyncContext(
      project = project,
      scope = scope,
      graph = syncStore.targetGraph,
      syncExecutor = this,
      languageService = service<SyncLanguageService>(),
      pathsResolver = project.connection.runWithServer { server -> server.workspaceBazelPaths().bazelPathsResolver },
      session = SyncSession(),
      flags = project.service<SyncFlagsService>(),
    )

    withTask(project, "sync_lifecycle_pre_events", "Executing pre-sync events") {
      for (listener in SyncLifecycleListener.ep.extensionList) {
        listener.onPreSync(ctx)
      }
    }

    val diff = withTask(project, "target_diff", "Computing incremental diff") {
      computeSyncDiff(ctx, scope)
    }

    withTask(project, "sync_lifecycle_events", "Executing sync events") {
      for (listener in SyncLifecycleListener.ep.extensionList) {
        listener.onSync(ctx, diff, SyncProgressReporter(this))
      }
    }

    withTask(project, "target_graph", "Updating target graph") {
      updateTargetGraph(ctx, diff)
    }

    withTask(project, "update_internal_indexes", "Updating internal indexes") {
      updateInternalIndexes(ctx, diff)
    }

    withTask(project, "update_workspace", "Updating workspace") {
      updateWorkspace(ctx, diff)
    }

    withTask(project, "sync_lifecycle_post_events", "Executing post-sync events") {
      for (listener in SyncLifecycleListener.ep.extensionList) {
        listener.onPostSync(ctx, SyncStatus.Success, SyncProgressReporter(this))
      }
    }

    withTask(project, "save_internal_stores", "Saving internal store") {
      //(project.service<BazelStorageService>().context as? LifecycleStoreContext)?.save(force = true)
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

  private suspend fun SyncConsoleTask.computeSyncDiff(ctx: SyncContext, scope: SyncScope): SyncDiff {
    val universeDiff = withTask("universe_diff", "Computing universe diff") {
      project.service<SyncUniverseService>().computeUniverseDiff(ctx, scope, SyncProgressReporter(this@withTask))
    }
    val vfsDiff = withTask("vfs_diff", "Computing VFS diff") {
      project.service<SyncVFSService>().computeVFSDiff(scope, universeDiff)
    }
    val scopeDiff = when (scope) {
      is SyncScope.Partial -> {
        val changed = scope.targets.filterIsInstance<PartialTarget.ByLabel>()
          .map { it.label }
          .toSet()
        SyncColdDiff(
          changed = changed,
          flags = HashMultimap.create<Label, SyncDiffFlags>().apply {
            for (changed in changed) {
              put(changed, SyncDiffFlags.FORCE_INVALIDATION)
            }
          },
        )
      }

      else -> SyncColdDiff()
    }
    val normalizer = SyncDiffNormalizer()
    val normalizedDiff = normalizer.normalize(listOf(vfsDiff, universeDiff, scopeDiff))
    val expandedDiff = withTask("expand_diff", "Computing dependency reachability") {
      project.service<SyncExpandService>().expandDependencyDiff(scope, normalizedDiff)
    }
    val useHasher = when {
      ctx.scope.isFullSync -> true
      !ctx.flags.useTargetHasher -> false
      else -> true
    }
    val coldDiff = if (useHasher) {
      withTask("hash_diff", "Computing hash diff") {
        project.service<SyncHasherService>().computeHashDiff(expandedDiff)
      }
    }
    else {
      expandedDiff
    }
    return withTask("baking diff", "Converting to hot diff") {
      val diff = normalizer.toHotDiff(ctx, coldDiff)
      val console = project.service<ConsoleService>().syncConsole
      console.addMessage("Targets added: ${diff.added.joinToString { it.label.toString() }}")
      console.addMessage("Targets removed: ${diff.removed.joinToString { it.label.toString() }}")
      console.addMessage("Targets changed: ${diff.changed.joinToString { it.label.toString() }}")
      diff
    }
  }

  private suspend fun SyncConsoleTask.updateTargetGraph(ctx: SyncContext, diff: SyncDiff) {
    val graph = ctx.graph

    val builder = SyncTargetBuilder(
      project = project,
      pathsResolver = LegacyBazelFrontendBridge.fetchBazelPathsResolver(project),
      legacyRepoMapping = LegacyBazelFrontendBridge.toLegacyRepoMapping(project.syncRepoMapping),
    )
    val targetsToFetch = (diff.added.map { it.label } + diff.changed.map { it.label })
      .toList()
    val rawTargets = fetchRawAspects(ctx, targetsToFetch)
    // TODO: at this point we already had to invoke `bazel build`
    //  that means we have in/out artifacts of each target
    //  from queries aren't able to detect ALL changed, after this step we should
    //   1. collect all in/out artifacts
    //   2. compare them to previous state
    //   3. update target diff accordingly
    //   4. update target graph ONLY with changed targets(for over vertices below should be only over changed targets)
    //  this step is OG plugin `artifact tracking` counterpart
    //  * this step should as lightweight as possible
    for (target in diff.removed + diff.changed.map { it.old }) {
      val id = graph.getVertexIdByLabel(target.label)
      if (id != EMPTY_ID) {
        graph.removeVertexById(id)
      }
    }
    val vertices = withTask("target_build", "Converting targets") {
      builder.buildAllChangedTargetVertices(ctx, rawTargets)
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

  private suspend fun SyncConsoleTask.updateWorkspace(
    ctx: SyncContext,
    diff: SyncDiff,
  ) {
    val syncActivityName =
      BazelPluginBundle.message(
        "console.task.sync.activity.name",
        BazelPluginConstants.BAZEL_DISPLAY_NAME,
      )
    val saveAndSyncHandler = serviceAsync<SaveAndSyncHandler>()
    UnindexedFilesScannerExecutor.getInstance(project).suspendScanningAndIndexingThenExecute(syncActivityName) {
      saveAndSyncHandler.disableAutoSave().use {
        withBackgroundProgress(project, BazelPluginBundle.message("background.progress.syncing.project"), true) {
          reportSequentialProgress {
            executeWorkspaceImport(ctx, diff, this@updateWorkspace)
          }
        }
      }
    }
    saveAndSyncHandler.scheduleProjectSave(project = project)
  }

  private suspend fun CoroutineScope.executeWorkspaceImport(ctx: SyncContext, diff: SyncDiff, console: SyncConsoleTask) {
    for (plugin in SyncLanguagePlugin.ep.extensionList) {
      if (!plugin.isEnabled(ctx)) {
        continue
      }

      console.withTask("importing_${plugin.language.name}", "Importing workspace - ${plugin.language.name}") {
        val importer = plugin.createWorkspaceImporter(ctx)
        importer.execute(ctx, diff, SyncProgressReporter(this@withTask))
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
        build = ctx.scope.build,
      )
    }
  }

}
