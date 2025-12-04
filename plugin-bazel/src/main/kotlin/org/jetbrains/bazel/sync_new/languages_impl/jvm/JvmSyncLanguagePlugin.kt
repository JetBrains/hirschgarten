package org.jetbrains.bazel.sync_new.languages_impl.jvm

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.lang.SyncLanguage
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDataBuilder
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDetector
import org.jetbrains.bazel.sync_new.lang.SyncLanguagePlugin
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.JvmSyncWorkspaceImporter
import org.jetbrains.bazel.sync_new.pipeline.SyncWorkspaceImporter
import org.jetbrains.bsp.protocol.RawAspectTarget

class JvmSyncLanguagePlugin : SyncLanguagePlugin<JvmSyncTargetData> {
  override val language: SyncLanguage
    get() = JvmSyncLanguage
  override val dataType: Class<out JvmSyncTargetData>
    get() = JvmSyncTargetData::class.java

  override suspend fun createLanguageDetector(ctx: SyncContext): SyncLanguageDetector = JvmSyncLanguageDetector

  override suspend fun createSyncDataBuilder(ctx: SyncContext): SyncLanguageDataBuilder<JvmSyncTargetData> =
    JvmSyncTargetBuilder()

  override suspend fun createWorkspaceImporter(ctx: SyncContext): SyncWorkspaceImporter = JvmSyncWorkspaceImporter()
}
