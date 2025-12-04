package org.jetbrains.bazel.sync_new.flow

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.graph.TargetReference

data class SyncDiff(
  val added: Set<TargetReference>,
  val removed: Set<TargetReference>,
  val changed: Set<ChangedTarget>,
) {
  val split: SplitDiff by lazy {
    val mergedChanged = added + changed.map { it.new }
    val mergedRemoved = removed + changed.map { it.old }
    SplitDiff(changed = mergedChanged, removed = mergedRemoved)
  }
}

data class ChangedTarget(
  val label: Label,
  val old: TargetReference,
  val new: TargetReference,
)

data class SplitDiff(
  val changed: Set<TargetReference>,
  val removed: Set<TargetReference>,
)

interface SyncTargetDiff {
  val added: Set<Label>
  val removed: Set<Label>
  val changed: Set<Label>
}

// SyncColdDiff only take into account 'flat' target changes
//  it does not include dependencies, also include only label changes no target data
class SyncColdDiff(
  val added: Set<Label> = emptySet(),
  val removed: Set<Label> = emptySet(),
  val changed: Set<Label> = emptySet(),
) {
  val universe: Set<Label> by lazy { added + removed + changed }
  val hasChanged: Boolean = added.isNotEmpty() || removed.isNotEmpty() || changed.isNotEmpty()
}

operator fun SyncColdDiff.plus(other: SyncColdDiff): SyncColdDiff {
  return SyncColdDiff(
    added = added + other.added,
    removed = removed + other.removed,
    changed = changed + other.changed,
  )
}
