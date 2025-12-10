package org.jetbrains.bazel.sync_new.languages_impl.jvm.importer

import com.google.common.hash.Hashing
import com.google.devtools.build.lib.view.proto.Deps
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.lang.getLangData
import org.jetbrains.bazel.sync_new.lang.store.IncrementalEntityStore
import org.jetbrains.bazel.sync_new.lang.store.modifyEntityTyped
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmSyncLanguage
import org.jetbrains.bazel.sync_new.languages_impl.jvm.JvmSyncTargetData
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.name

class JdepsAnalyzer(
  private val storage: IncrementalEntityStore<JvmResourceId, JvmModuleEntity>,
) {
  private val vertexId2JdepsCache = Int2ObjectOpenHashMap<Set<Path>>()

  suspend fun computeJdepsForChangedTargets(ctx: SyncContext, diff: SyncDiff) {
    // in previous logic all entities should have been pruned

    // create vertex filter
    val (added, _) = diff.split
    val targetIds = IntOpenHashSet()
    for (added in added) {
      val target = added.getBuildTarget() ?: continue
      targetIds.add(target.vertexId)
    }

    // walk entire graph to find topological order of vertices
    // and include only targets in filter
    val graph = ctx.graph
    val degree = Int2IntOpenHashMap()
    val queue = ArrayDeque<Int>() // TODO: create primitive collection for that
    val iter = graph.getAllVertexIds().iterator()
    while (iter.hasNext()) {
      val vertexId = iter.nextInt()
      val predecessors = graph.getPredecessors(vertexId).size
      if (predecessors == 0) {
        queue.addLast(vertexId)
      }
      degree.put(vertexId, predecessors)
    }

    val topoOrderedTargets = IntArrayList(targetIds.size)
    while (true) {
      val vertex = queue.removeFirstOrNull() ?: break
      if (targetIds.contains(vertex)) {
        topoOrderedTargets.add(vertex)
      }
      val successors = graph.getSuccessors(vertex)
      for (n in successors.indices) {
        val succ = successors.getInt(n)
        val deg = degree.addTo(succ, -1)
        if (deg == 1) {
          queue.addLast(succ)
        }
      }
    }

    val transitiveDepsCache = Int2ObjectOpenHashMap<Set<Path>>()

    // Clear the jdeps cache at the start to ensure fresh data
    vertexId2JdepsCache.clear()

    // process jdeps in reverse topological order
    //
    // jdeps are processed based on transitive dependencies
    // so we incrementally compute jdeps by getting all jdeps from targets
    // and the removing union of direct dependencies transitive jdeps
    for (n in topoOrderedTargets.size - 1 downTo 0) {
      val vertexId = topoOrderedTargets.getInt(n)
      val vertex = graph.getVertexById(vertexId) ?: continue
      val jvmData = JvmSyncLanguage.getLangData(vertex) ?: continue

      val thisTargetDeps = computeDependenciesFromTargetData(ctx, jvmData)

      val directTargetDeps = graph.getSuccessors(vertexId)
        .asSequence()
        .mapNotNull { graph.getVertexById(it) }
        .mapNotNull { JvmSyncLanguage.getLangData(it) }
        .flatMap { it.outputs.iJars + it.outputs.classJars + it.outputs.srcJars }
        .map { ctx.pathsResolver.resolve(it) }
        .toHashSet()

      // TODO: recheck if I can cache inside bfs
      val allTransitiveJdeps = transitiveDepsCache.computeIfAbsent(vertexId) {
        computeAllTransitiveJdepsClosure(ctx, vertexId)
          .flatMap { it }
          .toSet()
      }

      val thisTargetResolvedJDeps = thisTargetDeps - (directTargetDeps + allTransitiveJdeps)

      val vertexReferenceId = JvmResourceId.VertexReference(vertexId = vertexId)
      val jdepsTransitiveId = JvmResourceId.JdepsCache(vertexId = vertexId)
      storage.createEntity(jdepsTransitiveId) {
        JvmModuleEntity.JdepsCache(resourceId = it, myJdeps = thisTargetResolvedJDeps)
      }

      vertexId2JdepsCache.put(vertexId, thisTargetResolvedJDeps)
      storage.addDependency(vertexReferenceId, jdepsTransitiveId)

      val thisTargetOutputJars = (jvmData.outputs.classJars + jvmData.outputs.iJars)
        .map { ctx.pathsResolver.resolve(it) }
        .toHashSet()
      for (jdep in (thisTargetResolvedJDeps - thisTargetOutputJars)) {
        if (shouldSkipJdepsJar(jdep)) {
          continue
        }
        val libraryName = getSyntheticLibraryName(jdep)
        val jdepLibraryId = JvmResourceId.JdepsLibrary(libraryName = libraryName)
        val label = Label.synthetic(libraryName)
        storage.createEntity(jdepLibraryId) {
          JvmModuleEntity.LegacyLibraryModule(
            resourceId = it,
            label = label,
            dependencies = setOf(),
            interfaceJars = setOf(),
            classJars = setOf(jdep),
            sourceJars = setOf(),
            isFromInternalTarget = false,
            isLowPriority = false,
          )
        }
        storage.addDependency(vertexReferenceId, jdepLibraryId)

        // add jdep as dependency of target
        storage.modifyEntityTyped(
          JvmResourceId.VertexDeps(label = vertex.label),
        ) { deps: JvmModuleEntity.VertexDeps ->
          deps.copy(deps = deps.deps + label)
        }
      }
    }
  }

  private fun computeDependenciesFromTargetData(ctx: SyncContext, data: JvmSyncTargetData): Set<Path> {
    val result = mutableSetOf<Path>()
    for (deps in data.jvmTarget.jdeps) {
      val path = ctx.pathsResolver.resolve(deps)
      if (!Files.exists(path)) {
        continue
      }
      val deps = path.inputStream().use { Deps.Dependencies.parseFrom(it).dependencyList }
      result += deps.asSequence()
        .filter { it.isRelevant() }
        .map { ctx.pathsResolver.resolveOutput(Paths.get(it.path)) }
        .toSet()
    }
    return result
  }

  private fun computeAllTransitiveJdepsClosure(ctx: SyncContext, vertexId: Int) = sequence {
    val queue = ArrayDeque<Int>() // TODO: Use IntArrayFIFOQueue
    val visited = IntOpenHashSet()
    queue.addLast(vertexId)
    while (true) {
      val vertex = queue.removeFirstOrNull() ?: break

      val cachedJdeps = vertexId2JdepsCache.get(vertex)
      if (cachedJdeps != null) {
        yield(cachedJdeps)
      } else {
        val jdeps = storage.getEntity(JvmResourceId.JdepsCache(vertexId = vertex))
          as? JvmModuleEntity.JdepsCache
        if (jdeps != null) {
          vertexId2JdepsCache.put(vertex, jdeps.myJdeps)
          yield(jdeps.myJdeps)
        }
      }

      val succs = ctx.graph.getSuccessors(vertex)
      for (n in succs.indices) {
        val succ = succs.getInt(n)
        if (!visited.add(succ)) {
          continue
        }
        queue.addLast(succ)
      }
    }
  }

  private fun Deps.Dependency.isRelevant() = kind in sequenceOf(Deps.Dependency.Kind.EXPLICIT, Deps.Dependency.Kind.IMPLICIT)

  private fun shouldSkipJdepsJar(jar: Path): Boolean =
    jar.name.startsWith("header_") && jar.resolveSibling("processed_${jar.name.substring(7)}").exists()

  private fun getSyntheticLibraryName(lib: Path): String {
    val shaOfPath = Hashing.sha256()
      .hashString(lib.toString(), StandardCharsets.UTF_8)
      .toString()
      .take(7)
    return NameUtils.escape(lib.fileName.toString()) + "-" + shaOfPath
  }
}
