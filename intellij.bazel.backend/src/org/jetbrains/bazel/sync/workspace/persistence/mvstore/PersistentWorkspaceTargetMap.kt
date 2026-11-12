package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.BuildTargetInfo

internal class PersistentWorkspaceTargetMap(
  val partialSnapshot: PersistentWorkspaceSnapshot,
  val generation: SnapshotGeneration,
) : WorkspaceTargetMap {
  override fun findTargetInfoByKey(key: WorkspaceTargetKey): BuildTargetInfo? = generation.findTargetInfo(partialSnapshot, key)

  override fun allTargetInfos(): Sequence<BuildTargetInfo> = generation.scanAllTargetInfos(partialSnapshot)

  override fun findTargetByKey(key: WorkspaceTargetKey): BuildTarget? = generation.findOrLoadTarget(partialSnapshot, key)

  override fun allTargets(): Sequence<BuildTarget> = generation.scanAllTargets(partialSnapshot)

  override fun <T : BuildTargetData> findTargetData(key: WorkspaceTargetKey, type: Class<T>): T? =
    generation.findTargetData(partialSnapshot, key, type)
}
