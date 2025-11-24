package org.jetbrains.bazel.sync_new.flow

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.graph.TargetReference

data class SyncDiff(
  val added: Set<TargetReference>,
  val removed: Set<TargetReference>,
  val changed: Set<ChangedTarget>,
) {
  val split: SplitDiff by lazy {
    val mergedAdded = added + changed.map { it.new }
    val mergedRemoved = removed + changed.map { it.old }
    SplitDiff(added = mergedAdded, removed = mergedRemoved)
  }
}

data class ChangedTarget(
  val label: Label,
  val old: TargetReference,
  val new: TargetReference,
)

data class SplitDiff(
  val added: Set<TargetReference>,
  val removed: Set<TargetReference>,
)
