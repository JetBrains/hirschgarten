package org.jetbrains.bazel.sync_new.index

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff

interface SyncIndexUpdater {
  suspend fun updateIndexes(ctx: SyncContext, diff: SyncDiff)
}

interface SyncIndexUpdaterProvider {
  suspend fun createUpdaters(ctx: SyncContext): List<SyncIndexUpdater>

  companion object {
    val ep: ExtensionPointName<SyncIndexUpdaterProvider> = ExtensionPointName("org.jetbrains.bazel.syncIndexUpdaterProvider")
  }
}
