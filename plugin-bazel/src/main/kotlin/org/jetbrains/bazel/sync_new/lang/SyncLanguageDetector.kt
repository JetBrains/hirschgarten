package org.jetbrains.bazel.sync_new.lang

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bsp.protocol.RawAspectTarget

interface SyncLanguageDetector {
  fun detect(ctx: SyncContext, target: RawAspectTarget): Boolean
}
