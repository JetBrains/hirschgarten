package org.jetbrains.bazel.sync_new.flow.diff

import org.jetbrains.bazel.label.Label

// SyncColdDiff only take into account 'flat' target changes
//  it does not include dependencies, also include only label changes no target data
class SyncColdDiff(
  val added: Set<Label> = emptySet(),
  val removed: Set<Label> = emptySet(),
  val changed: Set<Label> = emptySet(),
) {
  val universe: Set<Label> by lazy { added + removed + changed }
}

operator fun SyncColdDiff.plus(other: SyncColdDiff): SyncColdDiff {
  return SyncColdDiff(
    added = added + other.added,
    removed = removed + other.removed,
    changed = changed + other.changed,
  )
}
