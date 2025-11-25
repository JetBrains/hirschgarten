package org.jetbrains.bazel.sync_new.flow.diff.vfs

import org.jetbrains.bazel.label.Label

// TODO: add too many changes - ignore VFS changes and recompute entire diff
class SyncFileDiff(
  val removed: List<SyncVFSFile>,
  val changed: List<SyncVFSFile>,
  val added: List<SyncVFSFile>,
)

data class WildcardFileDiff<T : SyncVFSFile>(
  val added: List<T>,
  val removed: List<T>,
  val changed: List<T>,
)

enum class SyncFileState {
  ADDED,
  REMOVED,
  CHANGED
}

// SyncColdDiff only take into account 'flat' target changes
//  it does not include dependencies
class SyncColdDiff(
  val added: Set<Label>,
  val removed: Set<Label>,
  val changed: Set<Label>
) {
  val universe: Set<Label> by lazy { added + removed + changed }
}
