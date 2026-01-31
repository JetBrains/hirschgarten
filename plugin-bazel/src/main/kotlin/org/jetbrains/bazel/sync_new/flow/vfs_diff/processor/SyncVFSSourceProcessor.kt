package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.google.common.collect.HashMultimap
import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.assumeResolved
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryArgs
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.consistentLabels
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.experimentalGraphlessQuery
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
import org.jetbrains.bazel.sync_new.flow.SyncDiffFlags
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFile
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFileContributor
import org.jetbrains.bazel.sync_new.flow.vfs_diff.WildcardFileDiff
import org.jetbrains.bazel.sync_new.storage.hash.hash
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
        ctx.storage.source2Target.put(hash(source), targets)
        // use nested foreach to use multimap put semantics
        for (target in targets) {
          target2Sources.computeIfAbsent(target) { mutableSetOf() }.add(source)
        }
      }
      for ((target, sources) in target2Sources) {
        ctx.storage.target2Source.put(hash(target), sources)
      }
      return SyncColdDiff()
    }

    val changed = mutableSetOf<Label>()

    for (remove in diff.removed) {
      val targets = ctx.storage.source2Target.remove(hash(remove.path)) ?: continue
      changed += targets
      for (target in targets) {
        ctx.storage.target2Source.remove(hash(target))
      }
    }

    val flags = HashMultimap.create<Label, SyncDiffFlags>()
    val contributors = SyncVFSFileContributor.ep.extensionList
    val changedSources = if (ctx.scope.build) {
      diff.changed
    }
    else {
      diff.changed.filter { file ->
        contributors.any {
          it.doesFileChangeInvalidateTarget(ctx.project, file.path)
        }
      }
    }
    val sources = (diff.added + changedSources).map { it.path }
    for ((source, targets) in runInverseSourceQuery(ctx, sources)) {
      for (target in targets) {
        if (ctx.flags.useFileChangeBasedInvalidation || ctx.scope.build) {
          flags.put(target, SyncDiffFlags.FORCE_INVALIDATION)
        }
        ctx.storage.source2Target.put(hash(source), target)
        ctx.storage.target2Source.compute(hash(target)) { _, v ->
          if (v == null) {
            mutableSetOf()
          }
          else {
            v.add(source)
            v
          }
        }
      }
      changed += targets
    }

    return SyncColdDiff(
      changed = changed,
      flags = flags,
    )
  }

  suspend fun runInverseSourceQuery(ctx: SyncVFSContext, sources: Collection<Path>): Map<Path, Set<Label>> {
    if (sources.isEmpty()) {
      return emptyMap()
    }
    val labels = SyncVFSLabelResolver.resolveSourceFileLabels(ctx, sources)
      .flatMap { it.value }
    if (labels.isEmpty()) {
      return emptyMap()
    }
    if (!ctx.flags.useOptimizedInverseSourceQuery) {
      return runFullInverseSourceQuery(ctx, labels)
    }

    val ops = listOf(
      suspend { runDirectInverseSourceQuery(ctx, labels) },
      suspend { runNarrowInverseSourceQuery(ctx, labels) },
      suspend { runFullInverseSourceQuery(ctx, labels) },
    )

    var result: Map<Path, Set<Label>> = mapOf()

    // try each query, in case of incomplete result widen query scope
    for (op in ops) {
      result = op()
      if (result.keys.containsAll(sources)) {
        // result is complete
        break
      }
    }

    // at this point we tried all queries, return whatever we have
    return result.filterKeys { it in sources }
  }

  suspend fun runDirectInverseSourceQuery(ctx: SyncVFSContext, sources: Collection<Label>): Map<Path, Set<Label>> {
    return runSourceMapQuery(ctx) {
      consistentLabels()
      noOrderOutput()
      experimentalGraphlessQuery()
      query("same_pkg_direct_rdeps(${sources.joinToString(separator = " + ") { it.toString() }})")
    }
  }

  suspend fun runNarrowInverseSourceQuery(ctx: SyncVFSContext, sources: Collection<Label>): Map<Path, Set<Label>> {
    val uniquePredecessorLabels = mutableSetOf<Label>()
    for (label in sources) {
      uniquePredecessorLabels += label.assumeResolved().copy(target = AllRuleTargets)
    }

    // query: rdeps(<unique packages>, <source files as labels>)
    val rdepsExpr = sources.joinToString(separator = " + ") { it.toString() }
    val rdepsUniverse = uniquePredecessorLabels.joinToString(separator = " ") { it.toString() }

    return runSourceMapQuery(ctx) {
      consistentLabels()
      noOrderOutput()
      if (ctx.flags.useSkyQueryForInverseSourceQueries) {
        universeScope(uniquePredecessorLabels.map { it.toString() })
      }
      else {
        experimentalGraphlessQuery()
      }
      query("rdeps(set($rdepsUniverse), $rdepsExpr, 1)")
    }
  }

  suspend fun runFullInverseSourceQuery(ctx: SyncVFSContext, sources: Collection<Label>): Map<Path, Set<Label>> {
    val universe = ctx.project.service<SyncUniverseService>().universe
    val rdepsExpr = sources.joinToString(separator = " + ") { it.toString() }
    val rdepsUniverse = SyncUniverseQuery.createUniverseQuery(universe.importState.patterns)
    return runSourceMapQuery(ctx) {
      consistentLabels()
      noOrderOutput()
      if (ctx.flags.useSkyQueryForInverseSourceQueries) {
        universeScope(SyncUniverseQuery.createSkyQueryUniverseScope(universe.importState.patterns))
      }
      else {
        experimentalGraphlessQuery()
      }
      query("rdeps($rdepsUniverse, $rdepsExpr)")
    }
  }

  suspend fun runSourceMapQuery(ctx: SyncVFSContext, builder: QueryArgs.() -> Unit): Map<Path, Set<Label>> {
    val connector = ctx.project.service<BazelConnectorService>().ofSyncTask(ctx.task)
    val result = connector.query {
      defaults()
      keepGoing()
      consistentLabels()
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
