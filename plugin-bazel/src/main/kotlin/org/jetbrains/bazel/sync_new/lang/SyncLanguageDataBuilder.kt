package org.jetbrains.bazel.sync_new.lang

import org.jetbrains.bazel.sync_new.bridge.LegacySyncTargetInfo
import org.jetbrains.bazel.sync_new.flow.SyncContext

interface SyncLanguageDataBuilder<T : SyncTargetData> {
  suspend fun init(ctx: SyncContext)
  suspend fun buildTargetData(ctx: SyncContext, target: LegacySyncTargetInfo): T?
}
