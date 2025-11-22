package org.jetbrains.bazel.sync_new.languages_impl.jvm

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.lang.SyncLanguage
import org.jetbrains.bazel.sync_new.lang.SyncLanguagePlugin
import org.jetbrains.bsp.protocol.RawAspectTarget

class JvmSyncLanguagePlugin : SyncLanguagePlugin<JvmSyncTargetData> {
  override val languages: List<SyncLanguage> = listOf(JvmSyncLanguage)
  override val dataClasses: List<Class<out JvmSyncTargetData>> = listOf(JvmSyncTargetData::class.java)

  override suspend fun isTargetSupported(
    ctx: SyncContext,
    target: RawAspectTarget,
  ): Boolean = target.target.hasJvmTargetInfo()

  override suspend fun createTargetData(
    ctx: SyncContext,
    target: RawAspectTarget,
  ): JvmSyncTargetData {


  }
}
