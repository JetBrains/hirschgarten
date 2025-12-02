package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.BazelSyncV2
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryArgs
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.noOrderOutput
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.universeScope
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.unwrapProtos
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseQuery
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseService
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFile
import org.jetbrains.bazel.sync_new.flow.vfs_diff.WildcardFileDiff
import org.jetbrains.bazel.sync_new.storage.put
import java.nio.file.Path

// TODO: I've observed that we can only incrementally update ONLY sync universe defined in projectview
//  if out universe would reference other source targets(outside universe) we won't be able to incrementally update them
//  I think we should keep this limitation, it greatly simplify incremental sync flow before dependency resolution
class SyncVFSSourceProcessor {
  suspend fun process(ctx: SyncVFSContext, diff: WildcardFileDiff<SyncVFSFile.SourceFile>): SyncColdDiff {
    if (ctx.scope.isFullSync || ctx.isFirstSync) {
      val sources = runSyncUniverseSourceQuery(ctx)
      val target2Sources = mutableMapOf<Label, MutableSet<Path>>()
      // populate with all target sources
      for ((source, targets) in sources) {
        ctx.storage.source2Target.put(source, targets)
        // use nested foreach to use multimap put semantics
        for (target in targets) {
          target2Sources.computeIfAbsent(target) { mutableSetOf() }.add(source)
        }
      }
      for ((target, sources) in target2Sources) {
        ctx.storage.target2Source.put(target, sources)
      }
      return SyncColdDiff()
    }

    val changed = mutableSetOf<Label>()

    for (remove in diff.removed) {
      val targets = ctx.storage.source2Target.remove(remove.path) ?: continue
      changed += targets
      for (target in targets) {
        ctx.storage.target2Source.remove(target)
      }
    }

    val sources = (diff.added + diff.changed).map { it.path }
    for ((source, targets) in runInverseSourceQuery(ctx, sources)) {
      for (target in targets) {
        ctx.storage.source2Target.put(source, target)
        ctx.storage.target2Source.put(target, source)
      }
      changed += targets
    }

    return SyncColdDiff(
      changed = changed,
    )
  }

  suspend fun runInverseSourceQuery(ctx: SyncVFSContext, sources: Collection<Path>): Map<Path, Set<Label>> {
    if (sources.isEmpty()) {
      return emptyMap()
    }
    if (!BazelSyncV2.useOptimizedInverseSourceQuery) {
      return runFullInverseSourceQuery(ctx, sources)
    }
    val sourceMap = runNarrowInverseSourceQuery(ctx, sources)
    return when {
      // consistency check
      sourceMap.keys.containsAll(sources) -> sourceMap
      else -> runFullInverseSourceQuery(ctx, sources)
    }
  }

  suspend fun runNarrowInverseSourceQuery(ctx: SyncVFSContext, sources: Collection<Path>): Map<Path, Set<Label>> {
    // TODO: properly handle resolveFullLabel failure
    val sourceLabels = SyncVFSLabelResolver.resolveSourceFileLabels(ctx, sources)
      .flatMap { it.value }
    val uniquePredecessorLabels = mutableSetOf<Label>()
    for (label in sourceLabels) {
      uniquePredecessorLabels += label.assumeResolved().copy(target = AllRuleTargets)
    }

    // query: rdeps(<unique packages>, <source files as labels>)
    val rdepsExpr = sourceLabels.joinToString(separator = " + ") { it.toString() }

    return runSourceMapQuery(ctx) {
      noOrderOutput()
      universeScope(uniquePredecessorLabels.map { it.toString() })
      query("allrdeps($rdepsExpr, 1)")
    }
  }

  suspend fun runFullInverseSourceQuery(ctx: SyncVFSContext, sources: Collection<Path>): Map<Path, Set<Label>> {
    val rdepsExpr = SyncVFSLabelResolver.resolveSourceFileLabels(ctx, sources)
      .flatMap { it.value }
      .joinToString(separator = " + ") { it.toString() }
    val universe = ctx.project.service<SyncUniverseService>().universe
    return runSourceMapQuery(ctx) {
      noOrderOutput()
      universeScope(SyncUniverseQuery.createSkyQueryUniverseScope(universe.importState.patterns))
      query("allrdeps($rdepsExpr, 1)")
    }
  }

  suspend fun runSourceMapQuery(ctx: SyncVFSContext, builder: QueryArgs.() -> Unit): Map<Path, Set<Label>> {
    val connector = ctx.project.service<BazelConnectorService>().ofLegacyTask()
    val result = connector.query {
      defaults()
      keepGoing()
      //consistentLabels()
      output(QueryOutput.PROTO)
      builder()
    }

    val targets = result.unwrap().unwrapProtos()
      .asSequence()
      .filter { it.hasRule() }
      .map { it.rule }
      .toList()
    val legacyRepoMapping = LegacyBazelFrontendBridge.toLegacyRepoMapping(ctx.repoMapping)
    val sourceMap = mutableMapOf<Path, MutableSet<Label>>()
    // TODO: parallelize?
    for (target in targets) {
      val targetLabel = Label.parse(target.name)
      val attr = target.attributeList.firstOrNull { it.name == "srcs" } ?: continue
      for (source in attr.stringListValueList) {
        val label = Label.parseOrNull(source) ?: continue
        val path = ctx.pathsResolver.toFilePath(label.assumeResolved(), legacyRepoMapping)
        sourceMap.getOrPut(path) { mutableSetOf() }.add(targetLabel)
      }
    }
    return sourceMap
  }

  suspend fun runSyncUniverseSourceQuery(ctx: SyncVFSContext): Map<Path, Set<Label>> {
    val universe = ctx.project.service<SyncUniverseService>().universe
    return runSourceMapQuery(ctx) {
      query("(${SyncUniverseQuery.createUniverseQuery(universe)})")
    }
  }
}
