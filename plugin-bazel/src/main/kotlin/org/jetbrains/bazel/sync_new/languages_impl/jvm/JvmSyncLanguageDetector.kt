package org.jetbrains.bazel.sync_new.languages_impl.jvm

import org.jetbrains.bazel.sync_new.bridge.LegacySyncTargetInfo
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDetector

object JvmSyncLanguageDetector : SyncLanguageDetector {
  override fun detect(
      ctx: SyncContext,
      target: LegacySyncTargetInfo,
  ): Boolean = target.target.hasJvmTargetInfo()
}
