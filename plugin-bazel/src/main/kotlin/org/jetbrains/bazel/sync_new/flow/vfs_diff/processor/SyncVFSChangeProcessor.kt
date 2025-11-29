package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.intellij.openapi.components.service
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncFileDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFile
import org.jetbrains.bazel.sync_new.flow.vfs_diff.WildcardFileDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.plus
import org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark.StarlarkLoadTrackerService

class SyncVFSChangeProcessor {
  suspend fun processBulk(ctx: SyncVFSContext, diff: SyncFileDiff, isPreFirstSync: Boolean): SyncColdDiff {
    val fullSyncDiff = if (ctx.scope.isFullSync || isPreFirstSync) {
      val starlarkDiff = ctx.project.service<StarlarkLoadTrackerService>()
        .computeFullStarlarkDiff(ctx)
      val sourceDiff = SyncVFSSourceProcessor()
        .computeFullSourceDiff(ctx)
      sourceDiff + starlarkDiff
    } else {
      SyncFileDiff()
    }

    val starlarkFileDiff = SyncVFSStarlarkFileProcessor().process(ctx, diff + fullSyncDiff)

    val buildFiles = filterDiff<SyncVFSFile.BuildFile>(diff) + starlarkFileDiff
    val buildFilesDiff = SyncVFSBuildFileProcessor().process(ctx, buildFiles)

    return buildFilesDiff
  }

  private inline fun <reified T : SyncVFSFile> filterDiff(diff: SyncFileDiff): WildcardFileDiff<T> {
    return WildcardFileDiff(
      removed = diff.removed.filterIsInstance<T>().toHashSet(),
      changed = diff.changed.filterIsInstance<T>().toHashSet(),
      added = diff.added.filterIsInstance<T>().toHashSet(),
    )
  }
}
