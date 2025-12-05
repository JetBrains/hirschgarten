package org.jetbrains.bazel.sync_new.languages_impl.kotlin

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDetector
import org.jetbrains.bsp.protocol.RawAspectTarget

object KotlinSyncLanguageDetector : SyncLanguageDetector {
  override fun detect(
    ctx: SyncContext,
    target: RawAspectTarget,
  ): Boolean = target.target.hasKotlinTargetInfo()
}
