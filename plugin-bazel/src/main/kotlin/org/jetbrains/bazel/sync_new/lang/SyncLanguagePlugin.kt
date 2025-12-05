package org.jetbrains.bazel.sync_new.lang

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.pipeline.SyncWorkspaceImporter

interface SyncLanguagePlugin<T : SyncTargetData> {
  val language: SyncLanguage<T>
  val dataType: Class<out T>

  fun isEnabled(ctx: SyncContext): Boolean = true
  suspend fun createLanguageDetector(ctx: SyncContext): SyncLanguageDetector
  suspend fun createSyncDataBuilder(ctx: SyncContext): SyncLanguageDataBuilder<T>
  suspend fun createWorkspaceImporter(ctx: SyncContext): SyncWorkspaceImporter

  companion object {
    val ep: ExtensionPointName<SyncLanguagePlugin<*>> = ExtensionPointName.create("org.jetbrains.bazel.syncLanguagePlugin")
  }
}
