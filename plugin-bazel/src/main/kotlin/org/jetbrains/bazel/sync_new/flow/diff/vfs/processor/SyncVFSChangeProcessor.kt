package org.jetbrains.bazel.sync_new.flow.diff.vfs.processor

import org.jetbrains.bazel.sync_new.flow.diff.vfs.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.diff.vfs.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.diff.vfs.SyncFileDiff
import org.jetbrains.bazel.sync_new.flow.diff.vfs.SyncVFSFile
import org.jetbrains.bazel.sync_new.flow.diff.vfs.WildcardFileDiff

class SyncVFSChangeProcessor{
  suspend fun processBulk(ctx: SyncVFSContext, diff: SyncFileDiff): SyncColdDiff {
    val buildFiles = filterDiff<SyncVFSFile.BuildFile>(diff)
    val buildFilesDiff = SyncVFSBuildFileProcessor().process(ctx, buildFiles)

    return buildFilesDiff
  }

  private inline fun <reified T : SyncVFSFile> filterDiff(diff: SyncFileDiff): WildcardFileDiff<T> {
    return WildcardFileDiff(
      removed = diff.removed.filterIsInstance<T>(),
      changed = diff.changed.filterIsInstance<T>(),
      added = diff.added.filterIsInstance<T>(),
    )
  }
}
