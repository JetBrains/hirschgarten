package org.jetbrains.bazel.sync.workspace.persistence

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.BuildTargetInfo
import org.jetbrains.bsp.protocol.data

@ApiStatus.Internal
interface WorkspaceTargetMap {
  fun findTargetInfoByKey(key: WorkspaceTargetKey): BuildTargetInfo?

  fun allTargetInfos(): Sequence<BuildTargetInfo>

  fun findTargetByKey(key: WorkspaceTargetKey): BuildTarget?

  @Deprecated(
    "Use findTargetByKey(WorkspaceTargetKey) or findTargetInfoByKey(WorkspaceTargetKey)",
    replaceWith = ReplaceWith("findTargetByKey()"),
    level = DeprecationLevel.ERROR,
  )
  fun findTargetByKey(key: WorkspaceTargetKey, options: TargetLoadOptions = TargetLoadOptions.ALL): BuildTarget? = findTargetByKey(key)

  fun allTargets(): Sequence<BuildTarget>

  @Deprecated(
    "Use allTargets() or allTargetInfos()",
    replaceWith = ReplaceWith("allTargets()"),
    level = DeprecationLevel.ERROR,
  )
  fun allTargets(options: TargetLoadOptions = TargetLoadOptions.ALL): Sequence<BuildTarget> = allTargets()

  /** Loads one language data of one target, without the other parts of the [BuildTarget]. */
  fun <T : BuildTargetData> findTargetData(key: WorkspaceTargetKey, type: Class<T>): T?

  companion object {
    val EMPTY: WorkspaceTargetMap = object : WorkspaceTargetMap {
      override fun findTargetInfoByKey(key: WorkspaceTargetKey): BuildTargetInfo? = null

      override fun allTargetInfos(): Sequence<BuildTargetInfo> = sequenceOf()

      override fun findTargetByKey(key: WorkspaceTargetKey): BuildTarget? = null

      override fun allTargets(): Sequence<BuildTarget> = sequenceOf()

      override fun <T : BuildTargetData> findTargetData(key: WorkspaceTargetKey, type: Class<T>): T? = null
    }
  }
}

@ApiStatus.Internal
class InMemoryWorkspaceTargetMap(val targets: Map<WorkspaceTargetKey, BuildTarget>) : WorkspaceTargetMap {
  override fun findTargetInfoByKey(key: WorkspaceTargetKey): BuildTargetInfo? = targets[key]?.info

  override fun allTargetInfos(): Sequence<BuildTargetInfo> = targets.values.asSequence().map { it.info }

  override fun findTargetByKey(key: WorkspaceTargetKey): BuildTarget? = targets[key]

  override fun allTargets(): Sequence<BuildTarget> = targets.values.asSequence()

  override fun <T : BuildTargetData> findTargetData(key: WorkspaceTargetKey, type: Class<T>): T? = targets[key]?.data(type)
}
