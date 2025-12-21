package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.flow.SyncProgressReporter
import org.jetbrains.bazel.sync_new.flow.SyncStatus
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityRemovalPropagator
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.lang.store.persistent.PersistentIncrementalEntityStore
import org.jetbrains.bazel.sync_new.languages_impl.jvm.importer.legacy.LegacyWorkspaceModelApplicator
import org.jetbrains.bazel.sync_new.pipeline.SyncWorkspaceImporter
import org.jetbrains.bazel.sync_new.storage.rocksdb.RocksdbKVStore

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

      //val iter = ((storage as PersistentIncrementalEntityStore<*, *>).resourceId2EntityStore as RocksdbKVStore<*, *>).values()
      //while (iter.hasNext()) {
      //  val value = iter.next()
      //  println(value)
      //}
      //iter.close()
    }

    progress.task.withTask("removing_entities", "Pruning old entities") {
      val (_, removed) = diff.split
      IncrementalEntityRemovalPropagator.remove(
        store = storage,
        removed = removed.mapNotNull { it.getBuildTarget() }
          .map { JvmResourceId.VertexReference(vertexId = it.vertexId) }
      )
    }

    computeVertexDepsEntities(diff)
    progress.task.withTask("collecting_jdeps", "Collecting jdeps") {
      JdepsAnalyzer(storage).computeJdepsForChangedTargets(ctx, diff)
    }
    progress.task.withTask("computing_modules", "Computing modules") {
      GeneratedSourcesProcessor(storage).computeGeneratedSources(ctx, diff)
      LibraryModuleProcessor(storage).computeLibraryModules(ctx, diff)
      SourceModuleProcessor(project, storage).computeSourceModules(ctx, diff)
    }
    KotlinStdlibProcessor(storage).computeKotlinStdlib(ctx, diff)

    if (USE_LEGACY_MODEL_APPLICATOR) {
      LegacyWorkspaceModelApplicator(storage).execute(ctx, progress)
    } else {
      error("new model applicator is not implemented yet")
    }

    return SyncStatus.Success
  }

  private fun computeVertexDepsEntities(diff: SyncDiff) {
    val (added, _) = diff.split
    for (added in added) {
      val target = added.getBuildTarget() ?: continue
      val vertexId = JvmResourceId.VertexReference(vertexId = target.vertexId)
      val resourceId = JvmResourceId.VertexDeps(label = added.label)
      storage.createEntity(resourceId) {
        JvmModuleEntity.VertexDeps(resourceId = it, deps = emptySet())
      }
      storage.addDependency(vertexId, resourceId)
    }
  }
}
