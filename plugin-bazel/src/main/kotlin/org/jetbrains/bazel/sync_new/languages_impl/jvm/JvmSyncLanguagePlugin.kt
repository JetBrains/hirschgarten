package org.jetbrains.bazel.sync_new.languages_impl.jvm

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.lang.SyncLanguage
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDetector
import org.jetbrains.bazel.sync_new.lang.SyncLanguagePlugin
import org.jetbrains.bsp.protocol.RawAspectTarget

class JvmSyncLanguagePlugin : SyncLanguagePlugin<JvmSyncTargetData> {
  override val language: SyncLanguage
    get() = JvmSyncLanguage
  override val languageDetector: SyncLanguageDetector
    get() = JvmSyncLanguageDetector
  override val dataType: Class<out JvmSyncTargetData>
    get() = JvmSyncTargetData::class.java


  override suspend fun createTargetData(
    ctx: SyncContext,
    target: RawAspectTarget,
  ): JvmSyncTargetData {
    TODO()
  }
}
