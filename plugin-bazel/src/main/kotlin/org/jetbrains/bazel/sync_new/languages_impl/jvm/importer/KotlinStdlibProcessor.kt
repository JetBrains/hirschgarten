package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.lang.getLangData
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.lang.store.modifyEntityTyped
import org.jetbrains.bazel.sync_new.languages_impl.kotlin.KotlinSyncLanguage

class KotlinStdlibProcessor(
  private val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity>,
) {
  private val stdlibSingletonLabel = Label.synthetic("rules_kotlin_kotlin-stdlibs")

  // TODO: port transitive dependency stdlib adding
  suspend fun computeKotlinStdlib(ctx: SyncContext, diff: SyncDiff) {
    val (added, removed) = diff.split
    for (added in added) {
      val target = added.getBuildTarget() ?: continue
      val kotlinData = KotlinSyncLanguage.getLangData(target) ?: continue
      storage.createEntity(JvmResourceId.KotlinStdlib) { resourceId ->
        val classJars = kotlinData.stdlibJars.map { ctx.pathsResolver.resolve(it) }
          .toSet()
        val inferredSourceJars = classJars
          .map { it.parent.resolve(it.fileName.toString().replace(".jar", "-sources.jar")) }
          .toSet()
        JvmModuleEntity.LegacyLibraryModule(
          resourceId = resourceId,
          label = stdlibSingletonLabel,
          dependencies = emptySet(),
          interfaceJars = emptySet(),
          classJars = classJars,
          sourceJars = inferredSourceJars,
          isFromInternalTarget = true,
          isLowPriority = true,
        )
      }
      storage.addDependency(JvmResourceId.VertexReference(vertexId = target.vertexId), JvmResourceId.KotlinStdlib)
      storage.modifyEntityTyped(JvmResourceId.VertexDeps(label = target.label)) { entity: JvmModuleEntity.VertexDeps ->
        entity.copy(deps = entity.deps + stdlibSingletonLabel)
      }
    }
  }
}
