package org.jetbrains.bazel.sync_new.flow.index

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.codec.ofInt
import org.jetbrains.bazel.sync_new.codec.ofSet
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.flow.SyncStoreService
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.index.One2ManyIndex
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdater
import org.jetbrains.bazel.sync_new.index.impl.createOne2ManyIndex
import org.jetbrains.bazel.sync_new.index.syncIndexService
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createKVStore
import org.jetbrains.bazel.sync_new.storage.hash.hash
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class SyncFileIndexService(
  private val project: Project,
) : SyncIndexUpdater {
  private val file2TargetId: One2ManyIndex<HashValue128, Int> = project.syncIndexService.createOne2ManyIndex(
    "file2TargetIndex",
  ) { name, ctx ->
    ctx.createKVStore<HashValue128, Set<Int>>(name, StorageHints.USE_PAGED_STORE)
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofSet(ofInt()) }
      .build()
  }

  private val syncStoreService by lazy { project.service<SyncStoreService>() }

  private val transitiveClosureService by lazy { project.service<TransitiveClosureIndexService>() }

  override suspend fun updateIndexes(ctx: SyncContext, diff: SyncDiff) {
    val pathsResolver = LegacyBazelFrontendBridge.fetchBazelPathsResolver(project)

    val (changed, removed) = diff.split
    for (removed in removed) {
      val target = removed.getBuildTarget() ?: continue
      for (source in target.genericData.sources) {
        val realPath = pathsResolver.resolve(source.path)
        file2TargetId.invalidate(hashFilePath(realPath), target.vertexId)
      }
    }

    for (changed in changed) {
      val target = changed.getBuildTarget() ?: continue
      for (source in target.genericData.sources) {
        val realPath = pathsResolver.resolve(source.path)
        file2TargetId.add(hashFilePath(realPath), listOf(target.vertexId))
      }
    }
  }

  fun getTargetVertexBySourceFile(file: Path): Sequence<BazelTargetVertex> {
    return file2TargetId.get(hashFilePath(file))
      .mapNotNull { syncStoreService.targetGraph.getVertexById(it) }
  }

  fun getTargetLabelsBySourceFile(file: Path): Sequence<Label> {
    return file2TargetId.get(hashFilePath(file))
      .mapNotNull { syncStoreService.targetGraph.getVertexCompactById(it) }
      .map { it.label }
  }

  fun getTargetIdsBySourceFile(file: Path): Sequence<Int> = file2TargetId.get(hashFilePath(file))

  fun getAllReverseExecutableTargetsBySourceFile(file: Path): Sequence<Label> {
    return getTargetIdsBySourceFile(file)
      .flatMap { vertexId -> transitiveClosureService.getAllReverseTransitiveExecutableTargetIds(vertexId) }
      .mapNotNull { syncStoreService.targetGraph.getVertexCompactById(it)?.label }
  }

  private fun hashFilePath(path: Path): HashValue128 = hash { putString(path.toString()) }

}
