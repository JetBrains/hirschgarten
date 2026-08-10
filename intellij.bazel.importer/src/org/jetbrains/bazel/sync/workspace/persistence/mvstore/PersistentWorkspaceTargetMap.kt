package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget

internal class PersistentWorkspaceTargetMap(
  val partialSnapshot: PersistentWorkspaceSnapshot,
  val generation: SnapshotGeneration,
) : WorkspaceTargetMap {
  override fun findTargetByKey(
    key: WorkspaceTargetKey,
    options: TargetLoadOptions,
  ): BuildTarget? = generation.findOrLoadTarget(partialSnapshot, key, options)

  override fun allTargets(options: TargetLoadOptions): Sequence<BuildTarget> =
    generation.scanAllTargets(partialSnapshot, options)
}
