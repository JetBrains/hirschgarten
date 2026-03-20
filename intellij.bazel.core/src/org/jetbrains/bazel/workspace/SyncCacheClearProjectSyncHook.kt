package org.jetbrains.bazel.workspace

import org.jetbrains.bazel.sync.ProjectPostSyncHook
import org.jetbrains.bazel.sync.SyncCache

internal class SyncCacheClearProjectSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    SyncCache.getInstance(environment.project).clear()
  }
}
