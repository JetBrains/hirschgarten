package org.jetbrains.bazel.sync_new.lang

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bsp.protocol.RawAspectTarget

interface SyncLanguageDataBuilder<T : SyncTargetData> {
  suspend fun init(ctx: SyncContext)
  suspend fun buildTargetData(ctx: SyncContext, target: RawAspectTarget): T?
}
