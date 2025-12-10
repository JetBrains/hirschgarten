package org.jetbrains.bazel.sync_new.flow.index

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdater

@Service(Service.Level.PROJECT)
class ExecutableTargetIndexService(
  private val project: Project
) : SyncIndexUpdater {
  override suspend fun updateIndexes(ctx: SyncContext, diff: SyncDiff) {
    // TODO
  }
}
