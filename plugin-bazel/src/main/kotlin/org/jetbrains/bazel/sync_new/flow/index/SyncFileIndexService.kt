package org.jetbrains.bazel.sync_new.flow.index

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import it.unimi.dsi.fastutil.longs.LongSet
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.bridge.LegacyBazelFrontendBridge
import org.jetbrains.bazel.sync_new.codec.ofHash128
import org.jetbrains.bazel.sync_new.codec.ofLabel
import org.jetbrains.bazel.sync_new.codec.ofLong
import org.jetbrains.bazel.sync_new.codec.ofSet
import org.jetbrains.bazel.sync_new.flow.SyncContext
import org.jetbrains.bazel.sync_new.flow.SyncDiff
import org.jetbrains.bazel.sync_new.flow.SyncScope
import org.jetbrains.bazel.sync_new.graph.impl.resolve
import org.jetbrains.bazel.sync_new.index.One2ManyIndex
import org.jetbrains.bazel.sync_new.index.SyncIndexUpdater
import org.jetbrains.bazel.sync_new.index.impl.createOne2ManyIndex
import org.jetbrains.bazel.sync_new.index.syncIndexService
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.createSortedKVStore
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.hash.hash128Comparator
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class SyncFileIndexService(
  private val project: Project,
) : SyncIndexUpdater {
  private val file2TargetIndex: One2ManyIndex<HashValue128, Long> = project.syncIndexService.createOne2ManyIndex(
    "file2TargetIndex",
  ) { name, ctx ->
    ctx.createSortedKVStore<HashValue128, Set<Long>>(name, StorageHints.USE_PAGED_STORE)
      .withKeyComparator { hash128Comparator() }
      .withKeyCodec { ofHash128() }
      .withValueCodec { ofSet(ofLong()) }
      .build()
  }

  override suspend fun updateIndexes(ctx: SyncContext, diff: SyncDiff) {
    val pathsResolver = LegacyBazelFrontendBridge.fetchBazelPathsResolver(project)

    val (added, removed) = diff.split

    val toRemove = mutableMapOf<HashValue128, MutableList<Long>>()
    for (removed in removed) {
      val target = removed.getBuildTarget() ?: continue
      for (source in target.genericData.sources) {
        val realPath = pathsResolver.resolve(source.path)
        val hash = hashFilePath(realPath)
        toRemove.getOrPut(hash) { mutableListOf() }.add(target.vertexId)
      }
    }

    for ((hash, vertexIds) in toRemove) {
      for (vertexId in vertexIds) {
        file2TargetIndex.invalidate(hash, vertexId)
      }
    }

    val toAdd = mutableMapOf<HashValue128, MutableList<Long>>()
    for (added in added) {
      val target = added.getBuildTarget() ?: continue
      for (source in target.genericData.sources) {
        val realPath = pathsResolver.resolve(source.path)
        val hash = hashFilePath(realPath)
        toAdd.getOrPut(hash) { mutableListOf() }.add(target.vertexId)
      }
    }

    for ((hash, vertexIds) in toAdd) {
      file2TargetIndex.add(hash, vertexIds)
    }
  }


  private fun hashFilePath(path: Path): HashValue128 = hash { putString(path.toString()) }

}
