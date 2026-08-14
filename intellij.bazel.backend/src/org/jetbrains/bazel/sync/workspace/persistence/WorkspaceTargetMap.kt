package org.jetbrains.bazel.sync.workspace.persistence

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget

@ApiStatus.Internal
interface WorkspaceTargetMap {
  fun findTargetByKey(key: WorkspaceTargetKey, options: TargetLoadOptions = TargetLoadOptions.ALL): BuildTarget?

  fun allTargets(options: TargetLoadOptions = TargetLoadOptions.ALL): Sequence<BuildTarget>

  companion object {
    val EMPTY: WorkspaceTargetMap = object : WorkspaceTargetMap {
      override fun findTargetByKey(
        key: WorkspaceTargetKey,
        options: TargetLoadOptions,
      ): BuildTarget? = null

      override fun allTargets(options: TargetLoadOptions): Sequence<BuildTarget> = sequenceOf()
    }
  }
}

@ApiStatus.Internal
interface WorkspaceTargetRef {
  val targetKey: WorkspaceTargetKey

  fun load(map: WorkspaceTargetMap, options: TargetLoadOptions): BuildTarget?

  companion object {
    fun of(key: WorkspaceTargetKey): WorkspaceTargetRef = object : WorkspaceTargetRef {
      override val targetKey: WorkspaceTargetKey
        get() = key

      override fun load(
        map: WorkspaceTargetMap,
        options: TargetLoadOptions,
      ): BuildTarget? = map.findTargetByKey(key, options)
    }
  }
}

@ApiStatus.Internal
class InMemoryWorkspaceTargetMap(val targets: Map<WorkspaceTargetKey, BuildTarget>) : WorkspaceTargetMap {
  override fun findTargetByKey(
    key: WorkspaceTargetKey,
    options: TargetLoadOptions,
  ): BuildTarget? = targets[key]

  override fun allTargets(options: TargetLoadOptions): Sequence<BuildTarget> = targets.values.asSequence()

}
