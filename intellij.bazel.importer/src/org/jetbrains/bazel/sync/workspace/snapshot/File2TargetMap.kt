package org.jetbrains.bazel.sync.workspace.snapshot

import com.dynatrace.hash4j.hashing.Hashing
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bsp.protocol.allSources
import java.nio.file.Path
import kotlin.io.path.relativeTo
import kotlin.io.path.relativeToOrSelf

/**
 * Immutable [Path] to [WorkspaceTarget] map
 */
@ApiStatus.Internal
class File2TargetMap internal constructor(
  private val workspaceRoot: Path?,
  private val hash2Targets: Long2ObjectMap<ArrayList<WorkspaceTargetKey>>,
) {

  companion object {
    val EMPTY: File2TargetMap = File2TargetMap(workspaceRoot = null, hash2Targets = Long2ObjectMaps.emptyMap())
  }

  fun getTargetsByFile(path: Path): List<WorkspaceTargetKey> {
    // try relative path first
    val relativePath = workspaceRoot?.let { path.relativeTo(it) }
    if (relativePath != null) {
      hash2Targets.get(hashFilePath(relativePath))
        ?.let { return it }
    }

    // then absolute path
    hash2Targets.get(hashFilePath(path))
      ?.let { return it }

    // otherwise empty
    return listOf()
  }

}

@ApiStatus.Internal
operator fun File2TargetMap.get(path: Path): List<WorkspaceTargetKey> = getTargetsByFile(path)

@ApiStatus.Internal
object File2TargetMapBuilder {
  fun build(workspaceRoot: Path? = null, targets: Iterable<WorkspaceTarget>): File2TargetMap {
    val hash2Targets = Long2ObjectOpenHashMap<ArrayList<WorkspaceTargetKey>>()
    for (target in targets) {
      for (source in target.rawBuildTarget.allSources) {
        // TODO: add more relativization paths to keep `File2TargetMap` portable
        val path = if (workspaceRoot == null) {
          source
        }
        else {
          source.relativeToOrSelf(source)
        }
        hash2Targets.computeIfAbsent(hashFilePath(path)) { ArrayList() }
          .add(target.targetKey)
      }
    }
    return File2TargetMap(
      workspaceRoot = workspaceRoot,
      hash2Targets = hash2Targets,
    )
  }

  @VisibleForTesting
  fun build(targets: Map<Path, List<WorkspaceTargetKey>>): File2TargetMap =
    File2TargetMap(workspaceRoot = null, Long2ObjectOpenHashMap(targets.entries.associate { (k, v) -> hashFilePath(k) to ArrayList(v) }))
}

private fun hashFilePath(path: Path): Long = Hashing.xxh3_64().hashStream().putString(path.toString()).asLong
