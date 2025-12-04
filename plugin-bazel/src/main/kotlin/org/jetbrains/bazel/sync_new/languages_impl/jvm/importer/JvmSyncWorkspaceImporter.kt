package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.flow.SyncProgressReporter
import org.jetbrains.bazel.sync_new.flow.SyncStatus
import org.jetbrains.bazel.sync_new.flow.universe.SyncUniverseService
import org.jetbrains.bazel.sync_new.flow.universe_expand.SyncExpandService
import org.jetbrains.bazel.sync_new.lang.store.persistent.createPersistentIncrementalEntityStore
import org.jetbrains.bazel.sync_new.pipeline.SyncWorkspaceImporter
import org.jetbrains.bazel.sync_new.storage.storageContext

class JvmSyncWorkspaceImporter(
  private val project: Project,
) : SyncWorkspaceImporter {

  internal val storage = createPersistentIncrementalEntityStore<JvmResourceId, JvmModuleEntity>(
    storageContext = project.storageContext,
    name = "jvm_sync_storage",
    resourceIdCodec = { ofKryo() },
    entityCodec = { ofKryo() },
    idHasher = { hash() },
  )

  override suspend fun execute(
    ctx: SyncContext,
    diff: SyncDiff,
    progress: SyncProgressReporter,
  ): SyncStatus {
    computeVertexDepsEntities(diff)

    return SyncStatus.Success
  }

  private fun isWorkspaceTarget(label: Label) = project.service<SyncExpandService>()
    .isWithinUniverseScope(label)

  private fun computeVertexDepsEntities(diff: SyncDiff) {
    val (added, removed) = diff.split
    for (added in added) {
      storage.createEntity(JvmResourceId.VertexDeps(label = added.label)) {
        JvmModuleEntity.VertexDeps(resourceId = it, deps = emptySet())
      }
    }
    for (removed in removed) {
      storage.removeEntity(JvmResourceId.VertexDeps(label = removed.label))
    }
  }
}
