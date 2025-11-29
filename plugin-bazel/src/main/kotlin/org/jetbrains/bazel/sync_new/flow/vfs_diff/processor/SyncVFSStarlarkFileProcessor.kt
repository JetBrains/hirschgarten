package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.intellij.openapi.components.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseService
import org.jetbrains.bazel.sync_new.flow.universe.isInsideUniverse
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncFileDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFile
import org.jetbrains.bazel.sync_new.flow.vfs_diff.WildcardFileDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark.StarlarkFileKind
import org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark.StarlarkFileNode
import org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark.StarlarkFileParser
import org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark.StarlarkLoadTrackerService
import org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark.StarlarkParsedFile
import kotlin.io.path.notExists
import kotlin.io.path.readText

// TODO: we can have custom rules for MODULE.bazel invalidation
//  for example modifying maven from jvm_rules_external would lead to @maven targets invalidation
class SyncVFSStarlarkFileProcessor {
  suspend fun process(ctx: SyncVFSContext, diff: SyncFileDiff): WildcardFileDiff<SyncVFSFile.BuildFile> {
    val trackerService = ctx.project.service<StarlarkLoadTrackerService>()
    val loadGraph = trackerService.starlarkLoadGraph

    processNewStarlarkFiles(
      ctx = ctx,
      starlarkFiles = (diff.added + diff.changed)
        .filterIsInstance<SyncVFSFile.StarlarkFile>(),
      buildFiles = (diff.added + diff.changed)
        .filterIsInstance<SyncVFSFile.BuildFile>(),
    )

    val changed = mutableSetOf<SyncVFSFile.BuildFile>()
    for (removed in diff.removed) {
      if (removed is SyncVFSFile.BuildFile) {
        continue
      }
      changed += loadGraph.getBuildPredecessors(removed.path)
        .map { SyncVFSFile.BuildFile(it.workspacePath) }
    }

    for (removed in diff.removed) {
      val node = loadGraph.getStarlarkFile(removed.path) ?: continue
      loadGraph.graph.removeVertex(node)
    }

    for (removed in diff.added + diff.changed) {
      if (removed is SyncVFSFile.BuildFile) {
        continue
      }
      changed += loadGraph.getBuildPredecessors(removed.path)
        .map { SyncVFSFile.BuildFile(it.workspacePath) }
    }

    return WildcardFileDiff(changed = changed)
  }

  suspend fun processNewStarlarkFiles(
    ctx: SyncVFSContext,
    starlarkFiles: Iterable<SyncVFSFile.StarlarkFile>,
    buildFiles: Iterable<SyncVFSFile.BuildFile>,
  ) {
    val trackerService = ctx.project.service<StarlarkLoadTrackerService>()
    val parsed = withContext(Dispatchers.IO) {
      val starlarkFiles = parseAll(trackerService.starlarkParser, StarlarkFileKind.STARLARK, starlarkFiles)
      val buildFiles = parseAll(trackerService.starlarkParser, StarlarkFileKind.BUILD, buildFiles)
      (starlarkFiles + buildFiles).awaitAll()
        .filterNotNull()
        .map { (file, kind) -> file to StarlarkFileNode(file.path, kind) }
    }
    val loadGraph = trackerService.starlarkLoadGraph.graph

    // add vertices
    for ((_, node) in parsed) {
      loadGraph.addVertex(node)
    }

    val legacyRepoMapping = LegacyBazelFrontendBridge.toLegacyRepoMapping(ctx.repoMapping)
    val universe = ctx.project.service<SyncUniverseService>().universe

    // add connections
    for ((file, node) in parsed) {
      for (load in file.loads) {
        if (load.isInsideUniverse(universe)) {
          val realLoadPath = ctx.pathsResolver.toFilePath(load.assumeResolved(), legacyRepoMapping)
          val to = trackerService.starlarkLoadGraph.getStarlarkFile(realLoadPath) ?: continue
          loadGraph.addEdge(node, to)
        }
      }
    }
  }

  private fun CoroutineScope.parseAll(
    starlarkParser: StarlarkFileParser,
    kind: StarlarkFileKind,
    list: Iterable<SyncVFSFile>,
  ): List<Deferred<Pair<StarlarkParsedFile, StarlarkFileKind>?>> {
    return list.map {
      async {
        return@async if (it.path.notExists()) {
          null
        } else {
          starlarkParser.parse(it.path.readText(), it.path) to kind
        }
      }
    }
  }
}
