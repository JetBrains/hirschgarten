package org.jetbrains.bazel.sync_new.pipeline

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff

interface SyncWorkspaceImporter {
  suspend fun import(ctx: SyncContext, diff: SyncDiff)
}
