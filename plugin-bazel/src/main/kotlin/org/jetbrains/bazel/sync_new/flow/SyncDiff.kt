package org.jetbrains.bazel.sync_new.flow

import org.jetbrains.bazel.sync_new.graph.TargetReference

data class SyncDiff(
  val added: Set<TargetReference>,
  val removed: Set<TargetReference>,
  val changed: Set<TargetReference>,
  val strategy: SyncScope,
)
