package org.jetbrains.bazel.sync.workspace.snapshot

import com.dynatrace.hash4j.hashing.Hashing
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.io.path.absolutePathString
import kotlin.io.path.relativeToOrNull
import kotlin.io.path.relativeToOrSelf

@ApiStatus.Internal
interface FileToTargetMap {
  fun getTargetsByFile(path: Path): List<WorkspaceTargetKey>

  // RC: this breaks immutability constrain of entire `WorkspaceSnapshot`
  // however for now it's needed, storage itself is not able to flush changes based on
  // snapshot diff, it's here for pure performance reason, we don't want to flush
  // entire snapshot each time we invoke bazel file listener
  fun addMapping(path: Path, targets: List<WorkspaceTargetKey>)
  fun removeMapping(path: Path)

  val size: Int

  companion object {
    val EMPTY: FileToTargetMap = object : FileToTargetMap {
      override fun getTargetsByFile(path: Path): List<WorkspaceTargetKey> = listOf()

      override fun addMapping(path: Path, targets: List<WorkspaceTargetKey>) {}

      override fun removeMapping(path: Path) {}

      override val size: Int
        get() = 0
    }
  }
}

@ApiStatus.Internal
class InMemoryFileToTargetMap internal constructor(
  private val hash2Targets: Long2ObjectMap<ArrayList<WorkspaceTargetKey>>,
) : FileToTargetMap {

  internal val delta = ConcurrentHashMap<Long, List<WorkspaceTargetKey>>()
  internal val lock = ReentrantReadWriteLock()

  override fun getTargetsByFile(path: Path): List<WorkspaceTargetKey> = lock.read {
    hash2Targets.get(hashFilePath(path))
      ?.let { return it }

    // otherwise empty
    return listOf()
  }

  override fun addMapping(path: Path, targets: List<WorkspaceTargetKey>): Unit = lock.write {
    val hash = hashFilePath(path)
    hash2Targets.put(hash, ArrayList(targets))
    delta[hash] = ArrayList(targets)
  }

  override fun removeMapping(path: Path): Unit = lock.write {
    val hash = hashFilePath(path)
    hash2Targets.remove(hash)
    delta[hash] = listOf()
  }

  override val size: Int
    get() = lock.read { hash2Targets.size }

}

@ApiStatus.Internal
operator fun FileToTargetMap.get(path: Path): List<WorkspaceTargetKey> = getTargetsByFile(path)

@ApiStatus.Internal
object File2TargetMapBuilder {
  fun build(targets: Iterable<BuildTarget>): FileToTargetMap {
    val hash2Targets = Long2ObjectOpenHashMap<ArrayList<WorkspaceTargetKey>>()
    for (target in targets) {
      for (source in target.allSources) {
        hash2Targets.computeIfAbsent(hashFilePath(source)) { ArrayList() }
          .add(target.key)
      }
    }
    return InMemoryFileToTargetMap(
      hash2Targets = hash2Targets,
    )
  }

  @VisibleForTesting
  fun build(targets: Map<Path, List<WorkspaceTargetKey>>): FileToTargetMap =
    InMemoryFileToTargetMap(
      hash2Targets = Long2ObjectOpenHashMap(targets.entries.associate { (k, v) -> hashFilePath(k) to ArrayList(v) }),
    )
}

private fun hashFilePath(path: Path): Long = Hashing.xxh3_64().hashStream().putString(path.absolutePathString()).asLong
