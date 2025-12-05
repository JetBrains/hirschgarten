package org.jetbrains.bazel.sync_new.languages_impl.kotlin

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.lang.SyncLanguage
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDataBuilder
import org.jetbrains.bazel.sync_new.lang.SyncLanguageDetector
import org.jetbrains.bazel.sync_new.lang.SyncLanguagePlugin
import org.jetbrains.bazel.sync_new.pipeline.SyncWorkspaceImporter

class KotlinSyncLanguagePlugin : SyncLanguagePlugin<KotlinSyncTargetData> {
  override val language: SyncLanguage<KotlinSyncTargetData> = KotlinSyncLanguage
  override val dataType: Class<out KotlinSyncTargetData> = KotlinSyncTargetData::class.java

  override suspend fun createLanguageDetector(ctx: SyncContext): SyncLanguageDetector = KotlinSyncLanguageDetector

  override suspend fun createSyncDataBuilder(ctx: SyncContext): SyncLanguageDataBuilder<KotlinSyncTargetData> = KotlinSyncTargetBuilder()

  override suspend fun createWorkspaceImporter(ctx: SyncContext): SyncWorkspaceImporter = KotlinSyncWorkspaceImporter()
}
