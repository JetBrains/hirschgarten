package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.lang.getLangData
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmSyncLanguage

class GeneratedSourcesProcessor(
  private val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity>
) {
  suspend fun computeGeneratedSources(ctx: SyncContext, diff: SyncDiff) {
    val (added, removed) = diff.split
    for (added in added) {
      val target = added.getBuildTarget() ?: continue
      val jvmData = JvmSyncLanguage.getLangData(target) ?: continue

      //storage.createEntity(JvmResourceId.CompiledLibrary())

    }
  }
}
