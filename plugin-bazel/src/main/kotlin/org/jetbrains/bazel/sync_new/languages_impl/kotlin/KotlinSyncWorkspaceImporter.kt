package org.jetbrains.bazel.sync_new.languages_impl.kotlin

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.flow.SyncProgressReporter
import org.jetbrains.bazel.sync_new.flow.SyncStatus
import org.jetbrains.bazel.sync_new.pipeline.SyncWorkspaceImporter

class KotlinSyncWorkspaceImporter : SyncWorkspaceImporter {
  override suspend fun execute(
    ctx: SyncContext,
    diff: SyncDiff,
    progress: SyncProgressReporter,
  ): SyncStatus {
    return SyncStatus.Success
  }
}
