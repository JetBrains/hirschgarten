package org.jetbrains.bazel.sync_new.flow.index

import com.intellij.openapi.components.serviceAsync
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.index.target_utils.TargetUtilsIndexService
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdater
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdaterProvider

class GenericSyncIndexUpdaterProvider : SyncIndexUpdaterProvider {
  override suspend fun createUpdaters(ctx: SyncContext): List<SyncIndexUpdater> = listOf(
    ctx.project.serviceAsync<SyncFileIndexService>(),
    ctx.project.serviceAsync<TransitiveClosureIndexService>(),
    ctx.project.serviceAsync<TargetTreeIndexService>(),
    ctx.project.serviceAsync<TargetUtilsIndexService>()
  )
}
