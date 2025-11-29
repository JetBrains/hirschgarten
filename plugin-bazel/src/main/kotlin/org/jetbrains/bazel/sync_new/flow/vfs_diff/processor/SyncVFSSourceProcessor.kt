package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.consistentLabels
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.unwrapProtos
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseQuery
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseService
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncFileDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFile
import org.jetbrains.bazel.sync_new.flow.vfs_diff.WildcardFileDiff
import java.nio.file.Path

// TODO: I've observed that we can only incrementally update ONLY sync universe defined in projectview
//  if out universe would reference other source targets(outside universe) we won't be able to incrementally update them
//  I think we should keep this limitation, it greatly simplify incremental sync flow before dependency resolution
class SyncVFSSourceProcessor {
  //suspend fun process(ctx: SyncVFSContext, diff: WildcardFileDiff<SyncVFSFile.SourceFile>): SyncColdDiff {
  //
  //}
  //
  //suspend fun runOptimizedInverseSourceQuery(ctx: SyncVFSContext, sources: List<Path>): Map<Path, Label> {
  //
  //}
  //
  //suspend fun runNaiveInverseSourceQuery(ctx: SyncVFSContext, sources: List<Path>): Map<Path, Label> {
  //
  //}

  suspend fun computeFullSourceDiff(ctx: SyncVFSContext): SyncFileDiff {
    val universe = ctx.project.service<SyncUniverseService>().universe
    val connector = ctx.project.service<BazelConnectorService>().ofLegacyTask()
    val result = connector.query {
      defaults()
      keepGoing()
      output(QueryOutput.PROTO)
      consistentLabels()
      query(SyncUniverseQuery.createUniverseQuery(universe))
    }
    val targets = result.unwrap().unwrapProtos()
      .asSequence()
      .filter { it.hasRule() }
      .map { it.rule }
    val legacyRepoMapping = LegacyBazelFrontendBridge.toLegacyRepoMapping(ctx.repoMapping)
    val added = mutableListOf<SyncVFSFile>()
    // TODO: parallelize?
    for (target in targets) {
      val sources = target.attributeList.firstOrNull { it.name == "srcs" } ?: continue
      sources.stringListValueList.map { Label.parse(it) }
        .map { ctx.pathsResolver.toFilePath(it.assumeResolved(), legacyRepoMapping) }
        .map { SyncVFSFile.SourceFile(it) }
        .forEach { added.add(it) }
    }
    return SyncFileDiff(
      added = added
    )
  }
}
