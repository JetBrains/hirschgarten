package org.jetbrains.bazel.sync_new.flow.universe_expand

import com.google.common.collect.HashMultimap
import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.connector.BazelConnectorService
import org.jetbrains.bazel.sync_new.connector.QueryOutput
import org.jetbrains.bazel.sync_new.connector.consistentLabels
import org.jetbrains.bazel.sync_new.connector.defaults
import org.jetbrains.bazel.sync_new.connector.keepGoing
import org.jetbrains.bazel.sync_new.connector.output
import org.jetbrains.bazel.sync_new.connector.query
import org.jetbrains.bazel.sync_new.connector.unwrap
import org.jetbrains.bazel.sync_new.connector.unwrapProtos
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.SyncDiffFlags
import org.jetbrains.bazel.sync_new.util.plus

class SyncExpandProcessor {
  suspend fun process(ctx: SyncExpandContext, diff: SyncColdDiff): SyncColdDiff {
    if (ctx.scope.isFullSync) {
      ctx.service.graph.reset()
    }
    val graph = ctx.service.graph.get()

    val added = mutableSetOf<Label>()
    val removed = mutableSetOf<Label>()
    val changed = mutableSetOf<Label>()

    added += diff.added
    changed += diff.changed
    removed += diff.removed

    val flags = HashMultimap.create<Label, SyncDiffFlags>()

    // invalidate direct reverse dependencies
    //for (removed in diff.removed) {
    //  val vertexId = graph.getOrAddVertex(removed)
    //  val predecessors = graph.getPredecessors(vertexId)
    //  for (n in predecessors.indices) {
    //    val predecessorId = predecessors.getInt(n)
    //    val predecessorLabel = graph.id2Label.get(predecessorId) ?: continue
    //    if (predecessorLabel !in diff.removed) {
    //      changed += predecessorLabel
    //      flags.put(predecessorLabel, SyncDiffFlags.FORCE_INVALIDATION)
    //    }
    //  }
    //}

    for (changed in (diff.added + diff.changed)) {
      graph.getOrAddVertex(changed)
      graph.addUniverseVertex(changed)
    }
    for (removed in diff.removed) {
      graph.removeUniverseVertex(removed)
    }

    val directDependencies = runDepsQuery(ctx, diff.added + diff.changed)

    // clear outgoing edges for changed nodes to remove stale dependencies
    for (changed in diff.changed) {
      val id = graph.label2Id.getInt(changed)
      if (id != SyncReachabilityGraph.EMPTY_ID) {
        // copy to array to avoid concurrent modification during iteration
        val successors = graph.getSuccessors(id).toIntArray()
        for (succ in successors) {
          graph.removeEdge(id, succ)
        }
      }
    }

    // TODO: consistency check
    for ((target, deps) in directDependencies) {
      // if vertex hasn't existed before - marked as newly discovered
      if (!graph.hasVertex(target)) {
        added += target
      }
      val fromId = graph.getOrAddVertex(target)
      for (dep in deps) {
        // same as above
        if (!graph.hasVertex(dep)) {
          added += dep
        }
        val toId = graph.getOrAddVertex(dep)
        graph.addEdge(fromId, toId)
      }
    }

    for (removed in diff.removed) {
      val removedId = graph.label2Id.getInt(removed)
      if (removedId == SyncReachabilityGraph.EMPTY_ID) {
        continue
      }
      graph.removeVertex(removedId)
    }

    // after modification recompute unreachable vertices
    // at this point unreachable vertices should be leftovers from diff application
    val unreachable = graph.computeUnreachableVertices()
    val unreachableLabels = unreachable.mapNotNull { graph.id2Label.get(it) }.toSet()
    val orphans = unreachableLabels.filter { it in added }.toSet()
    added -= unreachableLabels
    removed += (unreachableLabels - orphans)
    graph.removeAllVertices(unreachable)

    ctx.service.graph.mark()

    return SyncColdDiff(
      added = added,
      removed = removed,
      changed = changed,
      flags = flags + diff.flags
    )
  }

  // <target> -> <direct dependencies>
  suspend fun runDepsQuery(ctx: SyncExpandContext, targets: Collection<Label>): Map<Label, Set<Label>> {
    if (targets.isEmpty()) {
      return emptyMap()
    }
    val connector = ctx.project.service<BazelConnectorService>().ofLegacyTask()
    val result = connector.query {
      defaults()
      keepGoing()
      consistentLabels()
      output(QueryOutput.PROTO)
      query("deps(${targets.joinToString(separator = " + ") { it.toString() }})")
    }
    val targets = result.unwrap().unwrapProtos()
      .filter { it.hasRule() }
      .map { it.rule }
    val targetLabels = targets.map { Label.parse(it.name) }
      .toHashSet()
    val targetDependencies = mutableMapOf<Label, Set<Label>>()
    // TODO: make it parallel
    for (target in targets) {
      val label = Label.parse(target.name)
      val directInputs = target.ruleInputList.mapNotNull { Label.parseOrNull(it) }
        .filter { it in targetLabels }
        .toSet()
      targetDependencies[label] = directInputs
    }
    return targetDependencies
  }
}
