package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.dynatrace.hash4j.hashing.Hashing
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.absolutePathString

internal class PersistentFileToTargetMap(
  val partialSnapshot: PersistentWorkspaceSnapshot,
  val generation: SnapshotGeneration,

  /**
   * Live changes to [FileToTargetMap] performed using [FileToTargetMap.addMapping] and [FileToTargetMap.removeMapping],
   * are accumulated into [delta] map in order to prevent full rebuilds on entire [FileToTargetMap] on each small change.
   * This was mainly done to prevent full snapshot saves even on smallest changes performed by bazel file listener.
   */
  internal val delta: ConcurrentHashMap<Long, List<WorkspaceTargetKey>> = ConcurrentHashMap(),
) : FileToTargetMap {

  override fun getTargetsByFile(path: Path): List<WorkspaceTargetKey> {
    delta[hashFilePath(path)]?.let { return it }
    return generation.findTargetsByFile(partialSnapshot, path).toList()
  }

  override fun addMapping(path: Path, targets: List<WorkspaceTargetKey>) {
    delta[hashFilePath(path)] = targets
  }

  override fun removeMapping(path: Path) {
    delta[hashFilePath(path)] = listOf()
  }

  override val size: Int
    get() = generation.fileCount()  // stats-only, small pending deltas are not counted

  fun flush() {
    for ((hash, keys) in delta) {
      if (keys.isEmpty()) {
        generation.removeFileMapping(hash)
      }
      else {
        val keyIds = IntArrayList(keys.size)
        for (key in keys) {
          val keyId = partialSnapshot.keyId2Target.getReverseOrDefault(key, -1)
          if (keyId > 0) {
            keyIds.add(keyId)
          }
        }
        if (keyIds.isEmpty) {
          generation.removeFileMapping(hash)
        }
        else {
          generation.putFileMapping(hash, keyIds)
        }
      }
      delta.remove(hash, keys)
    }
  }
}

@ApiStatus.Internal
fun hashFilePath(path: Path): Long = Hashing.xxh3_64().hashStream().putString(path.absolutePathString()).asLong
