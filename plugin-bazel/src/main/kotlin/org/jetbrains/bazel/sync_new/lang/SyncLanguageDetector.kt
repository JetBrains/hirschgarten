package org.jetbrains.bazel.sync_new.lang

import org.jetbrains.bazel.sync_new.bridge.LegacySyncTargetInfo
import org.jetbrains.bazel.sync_new.flow.SyncContext

interface SyncLanguageDetector {
  fun detect(ctx: SyncContext, target: LegacySyncTargetInfo): Boolean
}
