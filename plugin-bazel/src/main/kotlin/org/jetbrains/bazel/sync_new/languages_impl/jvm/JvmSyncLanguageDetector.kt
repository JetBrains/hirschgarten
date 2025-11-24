package org.jetbrains.bazel.sync_new.languages_impl.jvm

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.lang.SyncLanguage
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDetector
import org.jetbrains.bsp.protocol.RawAspectTarget

object JvmSyncLanguageDetector : SyncLanguageDetector {
  override fun detect(
    ctx: SyncContext,
    target: RawAspectTarget,
  ): List<SyncLanguage> =
    if (target.target.hasJvmTargetInfo()) {
      listOf(JvmSyncLanguage)
    } else {
      listOf()
    }
}
