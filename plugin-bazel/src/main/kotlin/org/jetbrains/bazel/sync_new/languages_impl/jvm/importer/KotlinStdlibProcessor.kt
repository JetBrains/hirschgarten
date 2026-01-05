package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.graph.EMPTY_ID
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.lang.getLangData
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.lang.store.modifyEntityTyped
import org.jetbrains.bazel.sync_new.languages_impl.kotlin.KotlinSyncLanguage

class KotlinStdlibProcessor(
  private val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity>,
) {
  private val stdlibSingletonLabel = Label.synthetic("rules_kotlin_kotlin-stdlibs")

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

  suspend fun computeTransitiveKotlinStdlib(ctx: SyncContext, diff: SyncDiff) {
    val ktQueue = ArrayDeque<Int>() // TODO: replace with IntArrayFIFOQueue
    val allRoots = mutableListOf<Label>()
    val entities = storage.getAllEntities().toList()

    // cache graph field
    val graph = ctx.graph

    // collect roots
    for (entity in entities) {
      when (entity) {
        is JvmModuleEntity.LegacySourceModule -> {
          if (entity.legacyKotlinData != null) {
            val id = graph.getVertexIdByLabel(label = entity.label)
            if (id != EMPTY_ID) {
              ktQueue.addLast(id)
            }
          }
          allRoots += entity.label
        }

        else -> {
          /* noop */
        }
      }
    }

    // remove all stdlib dependencies
    for (label in allRoots) {
      storage.modifyEntityTyped(JvmResourceId.VertexDeps(label = label)) { deps: JvmModuleEntity.VertexDeps ->
        if (stdlibSingletonLabel in deps.deps) {
          deps.copy(deps = deps.deps - stdlibSingletonLabel)
        }
        else {
          deps
        }
      }
    }

    // transitively propagate stdlib from kt roots
    val queue = ArrayDeque(ktQueue)
    val visited = IntOpenHashSet()
    while (true) {
      val target = queue.removeFirstOrNull()
      if (target == null) {
        break
      }
      if (!visited.add(target)) {
        continue
      }
      val label = graph.getLabelByVertexId(target) ?: continue
      storage.modifyEntityTyped(JvmResourceId.VertexDeps(label = label)) { entity: JvmModuleEntity.VertexDeps ->
        entity.copy(deps = entity.deps + stdlibSingletonLabel)
      }
      val iter = graph.getPredecessors(target).intIterator()
      while (iter.hasNext()) {
        val element = iter.nextInt()
        if (!visited.contains(element)) {
          queue.addLast(element)
        }
      }
    }
  }
}
