package org.jetbrains.bazel.sync_new.pipeline

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.flow.SyncProgressReporter
import org.jetbrains.bazel.sync_new.flow.SyncStatus

interface SyncWorkspaceImporter {
  suspend fun execute(ctx: SyncContext, diff: SyncDiff, progress: SyncProgressReporter): SyncStatus
}
