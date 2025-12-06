package org.jetbrains.bazel.sync_new.flow

import com.intellij.openapi.extensions.ExtensionPointName

interface SyncLifecycleListener {
  suspend fun onPreSync(ctx: SyncContext) {
    /* noop */
  }

  suspend fun onSync(ctx: SyncContext, diff: SyncDiff, progress: SyncProgressReporter) {
    /* noop */
  }

  suspend fun onPostSync(ctx: SyncContext, status: SyncStatus, progress: SyncProgressReporter) {
    /* noop */
  }

  companion object {
    val ep: ExtensionPointName<SyncLifecycleListener> = ExtensionPointName("org.jetbrains.bazel.syncLifecycleListener")
  }
}
