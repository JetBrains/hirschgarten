package org.jetbrains.bazel.workspace

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.SyncCache

internal class CleanupLabelsCacheSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    Label.cleanInternCache()
  }
}
