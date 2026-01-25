package org.jetbrains.bazel.sync_new.flow.vfs_diff.processor

import com.google.common.collect.HashMultimap
import com.intellij.openapi.components.service
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.SyncColdDiff
import org.jetbrains.bazel.sync_new.flow.SyncDiffFlags
import org.jetbrains.bazel.sync_new.flow.plus
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSContext
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncFileDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.SyncVFSFile
import org.jetbrains.bazel.sync_new.flow.vfs_diff.WildcardFileDiff
import org.jetbrains.bazel.sync_new.flow.vfs_diff.plus
import org.jetbrains.bazel.sync_new.flow.vfs_diff.starlark.StarlarkLoadTrackerService
import org.jetbrains.bazel.sync_new.util.plus

class SyncVFSChangeProcessor {
  suspend fun processBulk(ctx: SyncVFSContext, diff: SyncFileDiff): SyncColdDiff {
    val universeDiff = ctx.project.service<StarlarkLoadTrackerService>()
      .computeStarlarkDiffFromUniverseDiff(ctx)

    val starlarkFileDiff = SyncVFSStarlarkFileProcessor().process(ctx, diff + universeDiff)

    val buildFiles = filterDiff<SyncVFSFile.BuildFile>(diff + universeDiff) + starlarkFileDiff
    val buildFilesDiff = SyncVFSBuildFileProcessor().process(ctx, buildFiles)

    val sourceFiles = filterDiff<SyncVFSFile.SourceFile>(diff)
    val sourceFilesDiff = SyncVFSSourceProcessor().process(ctx, sourceFiles)

    val diff = buildFilesDiff + sourceFilesDiff
    if (ctx.scope.isPartial) {
      val flags = HashMultimap.create<Label, SyncDiffFlags>()
      diff.changed.forEach { flags.put(it, SyncDiffFlags.FORCE_INVALIDATION) }
      return diff.copy(flags = diff.flags + flags)
    }
    return diff
  }

  private inline fun <reified T : SyncVFSFile> filterDiff(diff: SyncFileDiff): WildcardFileDiff<T> {
    return WildcardFileDiff(
      removed = diff.removed.filterIsInstance<T>().toHashSet(),
      changed = diff.changed.filterIsInstance<T>().toHashSet(),
      added = diff.added.filterIsInstance<T>().toHashSet(),
    )
  }
}
