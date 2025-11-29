package org.jetbrains.bazel.sync_new.flow.vfs_diff

import org.jetbrains.bazel.label.Label

// TODO: add too many changes - ignore VFS changes and recompute entire diff
class SyncFileDiff(
  val added: List<SyncVFSFile> = emptyList(),
  val removed: List<SyncVFSFile> = emptyList(),
  val changed: List<SyncVFSFile> = emptyList(),
)

data class WildcardFileDiff<T : SyncVFSFile>(
  val added: Set<T> = emptySet(),
  val removed: Set<T> = emptySet(),
  val changed: Set<T> = emptySet(),
)

// SyncColdDiff only take into account 'flat' target changes
//  it does not include dependencies
// TODO: refactor out of VFS changes and make it able to exchange this part of diffing
//  so instead of making diffs using VFS we can switch to other technique
class SyncColdDiff(
  val added: Set<Label> = emptySet(),
  val removed: Set<Label> = emptySet(),
  val changed: Set<Label> = emptySet(),
) {
  val universe: Set<Label> by lazy { added + removed + changed }
}

operator fun <T : SyncVFSFile> WildcardFileDiff<T>.plus(other: WildcardFileDiff<T>): WildcardFileDiff<T> {
  return WildcardFileDiff(
    added = added + other.added,
    removed = removed + other.removed,
    changed = changed + other.changed,
  )
}

operator fun SyncFileDiff.plus(other: SyncFileDiff): SyncFileDiff {
  return SyncFileDiff(
    added = added + other.added,
    removed = removed + other.removed,
    changed = changed + other.changed,
  )
}

operator fun SyncColdDiff.plus(other: SyncColdDiff): SyncColdDiff {
  return SyncColdDiff(
    added = added + other.added,
    removed = removed + other.removed,
    changed = changed + other.changed,
  )
}

