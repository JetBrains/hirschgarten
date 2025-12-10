package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.lang.getLangData
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.lang.store.modifyEntityTyped
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmSyncLanguage

class GeneratedSourcesProcessor(
  private val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity>
) {
  suspend fun computeGeneratedSources(ctx: SyncContext, diff: SyncDiff) {
    val (added, _) = diff.split
    for (added in added) {
      val target = added.getBuildTarget() ?: continue
      val jvmData = JvmSyncLanguage.getLangData(target) ?: continue

      if (jvmData.jvmTarget.generatedSources.isNotEmpty()) {
        val resourceId = JvmResourceId.GeneratedLibrary(owner = target.vertexId, name = "annotation_processor")
        val syntheticLabels = Label.synthetic(target.label.toString() + "_annotation_processor")
        storage.createEntity(resourceId) { resourceId ->
          JvmModuleEntity.LegacyLibraryModule(
            resourceId = resourceId,
            label = syntheticLabels,
            dependencies = emptySet(),
            interfaceJars = jvmData.outputs.iJars
              .map { ctx.pathsResolver.resolve(it) }
              .toSet(),
            classJars = jvmData.outputs.classJars
              .map { ctx.pathsResolver.resolve(it) }
              .toSet(),
            sourceJars = jvmData.outputs.srcJars
              .map { ctx.pathsResolver.resolve(it) }
              .toSet(),
            isFromInternalTarget = true,
            isLowPriority = false
          )
        }
        storage.modifyEntityTyped(JvmResourceId.VertexDeps(label = target.label)) { deps: JvmModuleEntity.VertexDeps ->
          deps.copy(deps = deps.deps + syntheticLabels)
        }
      }
    }
  }
}
