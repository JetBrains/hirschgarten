package org.jetbrains.bazel.sync.workspace.persistence

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTargetData
import kotlin.reflect.KClass

@ApiStatus.Internal
interface WorkspaceTargetMap {
  fun findTargetByKey(key: WorkspaceTargetKey, options: TargetLoadOptions = TargetLoadOptions.DEFAULT): WorkspaceTarget?

  fun allTargets(options: TargetLoadOptions = TargetLoadOptions.DEFAULT): Sequence<WorkspaceTarget>

  companion object {
    val EMPTY: WorkspaceTargetMap = object : WorkspaceTargetMap {
      override fun findTargetByKey(
        key: WorkspaceTargetKey,
        options: TargetLoadOptions,
      ): WorkspaceTarget? = null

      override fun allTargets(options: TargetLoadOptions): Sequence<WorkspaceTarget> = sequenceOf()
    }
  }
}

@ApiStatus.Internal
interface WorkspaceTargetRef {
  val targetKey: WorkspaceTargetKey

  fun load(map: WorkspaceTargetMap, options: TargetLoadOptions): WorkspaceTarget?

  companion object {
    fun of(key: WorkspaceTargetKey): WorkspaceTargetRef = object : WorkspaceTargetRef {
      override val targetKey: WorkspaceTargetKey
        get() = key

      override fun load(
        map: WorkspaceTargetMap,
        options: TargetLoadOptions,
      ): WorkspaceTarget? = map.findTargetByKey(key, options)
    }
  }
}

@ApiStatus.Internal
enum class TargetLoadHint {
  // skip loading expensive file collections
  SKIP_FILE_SETS,

  // skip loading dependencies
  SKIP_DEPS,

  // skip loading language specific target data
  SKIP_TARGET_DATA
}

@ApiStatus.Internal
data class TargetLoadOptions(
  val hints: Set<TargetLoadHint>,

  // taken into account only when hints has `SKIP_TARGET_DATA`
  val dataTypes: Set<KClass<out BuildTargetData>>,
) {
  companion object {
    // default mean entire target
    val DEFAULT: TargetLoadOptions = TargetLoadOptions(hints = setOf(), dataTypes = setOf())
  }
}

@ApiStatus.Internal
fun TargetLoadOptions(vararg hints: TargetLoadHint, types: Set<KClass<out BuildTargetData>> = setOf()): TargetLoadOptions =
  TargetLoadOptions(hints.toSet(), types)

@ApiStatus.Internal
class InMemoryWorkspaceTargetMap(val targets: Map<WorkspaceTargetKey, WorkspaceTarget>) : WorkspaceTargetMap {
  override fun findTargetByKey(
    key: WorkspaceTargetKey,
    options: TargetLoadOptions,
  ): WorkspaceTarget? = targets[key]

  override fun allTargets(options: TargetLoadOptions): Sequence<WorkspaceTarget> = targets.values.asSequence()

}
