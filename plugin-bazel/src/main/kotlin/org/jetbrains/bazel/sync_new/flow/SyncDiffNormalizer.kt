package org.jetbrains.bazel.sync_new.flow

import org.jetbrains.bazel.sync_new.graph.TargetReference

class SyncDiffNormalizer {
  fun normalize(diff: Collection<SyncColdDiff>): SyncColdDiff {
    val added = diff.flatMap { it.added }
    val removed = diff.flatMap { it.removed }
    val changed = diff.flatMap { it.changed }
    return SyncColdDiff(
      added = added.toSet(),
      removed = removed.toSet(),
      changed = changed.filter { it !in added }
        .toSet(),
    )
  }

  fun toHotDiff(ctx: SyncContext, diff: SyncColdDiff): SyncDiff {
    val graph = ctx.graph
    return SyncDiff(
      added = diff.added.map { TargetReference.ofGraphLazy(it, graph) }.toSet(),
      removed = diff.removed.map { TargetReference.ofGraphNow(it, graph) }.toSet(),
      changed = diff.changed.map {
        ChangedTarget(
          label = it,
          old = TargetReference.ofGraphNow(it, graph),
          new = TargetReference.ofGraphLazy(it, graph),
        )
      }.toSet(),
    )
  }
}
