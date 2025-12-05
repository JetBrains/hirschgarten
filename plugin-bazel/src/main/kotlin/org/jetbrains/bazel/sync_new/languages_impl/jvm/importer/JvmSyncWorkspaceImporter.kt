package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.codec.kryo.ofKryo
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.flow.SyncProgressReporter
import org.jetbrains.bazel.sync_new.flow.SyncStatus
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.lang.store.persistent.createPersistentIncrementalEntityStore
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.legacy.LegacyWorkspaceModelApplicator
import org.jetbrains.bazel.sync_new.pipeline.SyncWorkspaceImporter
import org.jetbrains.bazel.sync_new.storage.storageContext

@Service(Service.Level.PROJECT)
class JvmSyncWorkspaceImporter(
  private val project: Project,
) : SyncWorkspaceImporter {

  companion object {
    private const val USE_LEGACY_MODEL_APPLICATOR = true
  }

  val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity> = project.service<JvmSyncWorkspaceService>().storage

  override suspend fun execute(
    ctx: SyncContext,
    diff: SyncDiff,
    progress: SyncProgressReporter,
  ): SyncStatus {
    if (ctx.scope.isFullSync) {
      storage.clear()
    }

    computeVertexDepsEntities(diff)
    JdepsAnalyzer(storage).computeJdepsForChangedTargets(ctx, diff)
    SourceModuleProcessor(project, storage).computeSourceModules(ctx, diff)

    val (_, removed) = diff.split
    for (removed in removed) {
      val target = removed.getBuildTarget() ?: continue
      val resourceId = JvmResourceId.VertexReference(vertexId = target.vertexId)
      for (dependency in storage.getTransitiveDependants(resourceId).toList()) {
        val referrers = storage.getDirectReferrers(dependency).toList()
        val canBeRemoved = referrers.isEmpty() || (referrers.size == 1 && referrers.first() == resourceId)
        if (canBeRemoved) {
          storage.removeEntity(dependency)
        }
      }
    }

    if (USE_LEGACY_MODEL_APPLICATOR) {
      LegacyWorkspaceModelApplicator(storage).execute(ctx, progress)
    } else {
      error("new model applicator is not implemented yet")
    }

    return SyncStatus.Success
  }

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
