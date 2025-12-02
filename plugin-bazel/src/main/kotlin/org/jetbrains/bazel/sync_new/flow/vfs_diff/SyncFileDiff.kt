package org.jetbrains.bazel.sync_new.flow.vfs_diff

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

