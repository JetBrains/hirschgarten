package org.jetbrains.bazel.sync.workspace.snapshot

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.label.assumeResolved
import java.util.concurrent.atomic.AtomicReferenceArray

/**
 * Immutable [WorkspaceTarget] graph, takes into account configurations [WorkspaceConfiguration]
 */
@ApiStatus.Internal
interface WorkspaceTargetGraph {

  /**
   * Entire set of [WorkspaceTarget] present inside this [WorkspaceTargetGraph]
   */
  val allTargets: Array<WorkspaceTarget>

  /**
   * Find [WorkspaceTarget] by distinct [WorkspaceTargetKey]
   *
   * @param targetKey Specific [WorkspaceTargetKey]
   */
  fun findTargetByKey(targetKey: WorkspaceTargetKey): WorkspaceTarget?

  /**
   * Walk graph and find all targets until depth [maxDepth]
   *
   * @param maxDepth Max walk depth
   * @param runtimeDependencies Do include all runtime dependencies
   * @param condition Include targets that satisfy this condition
   *
   * @return Target list in topological order, according to [maxDepth] setting
   */
  fun findAllTargetsAtDepth(
    maxDepth: Int,
    runtimeDependencies: Boolean = maxDepth < 0,
    condition: (key: WorkspaceTargetKey) -> Boolean = { true },
  ): List<WorkspaceTarget>

  /**
   * Find all transitive successors to [targetKey]
   *
   * @param targetKey Target key
   *
   * @return Sequence of all transitive successors of [targetKey]
   */
  fun findAllTransitiveSuccessors(targetKey: WorkspaceTargetKey): Sequence<WorkspaceTarget>

  /**
   * Find all transitive successors to [targetKey] excluding traversal over root targets
   *
   * @param targetKey Target key
   *
   * @return Sequence of all transitive successors of [targetKey] exclusing root targets
   */
  fun findAllTransitiveSuccessorsWithoutRootTargets(targetKey: WorkspaceTargetKey): Sequence<WorkspaceTarget>

}

internal const val INVALID_TARGET_ID: Int = -1

/**
 * Low memory footprint, immutable target graph representation
 */
internal class WorkspaceTargetGraphImpl internal constructor(
  private val rootTargetIds: IntSet,
  private val targetKey2TargetId: Object2IntMap<WorkspaceTargetKey>,
  private val id2WorkspaceTarget: Array<WorkspaceTarget>,
  private val id2CompileSuccessors: Array<IntArray>,
  private val id2AllSuccessors: Array<IntArray>,
) : WorkspaceTargetGraph {
  private val id2TransitiveTargetCache: AtomicReferenceArray<IntArray?> = AtomicReferenceArray(targetKey2TargetId.size)

  override val allTargets: Array<WorkspaceTarget> = id2WorkspaceTarget

  override fun findTargetByKey(targetKey: WorkspaceTargetKey): WorkspaceTarget? {
    val targetId = targetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID)
    if (targetId == INVALID_TARGET_ID) {
      return null
    }
    else {
      return id2WorkspaceTarget[targetId]
    }
  }

  override fun findAllTargetsAtDepth(
    maxDepth: Int,
    runtimeDependencies: Boolean,
    condition: (key: WorkspaceTargetKey) -> Boolean,
  ): List<WorkspaceTarget> {
    val depth = IntArray(id2WorkspaceTarget.size) { Int.MAX_VALUE }

    // TODO: use primitive collection here
    val toVisit = ArrayDeque<Int>()
    val result = mutableListOf<WorkspaceTarget>()

    // perform BFS traversal
    for (targetId in rootTargetIds.intIterator()) {
      val target = id2WorkspaceTarget[targetId]
      if (!condition(target.targetKey)) {
        continue
      }
      toVisit.add(targetId)
      depth[targetId] = 0
    }

    while (toVisit.isNotEmpty()) {
      val currentId = toVisit.removeFirst()
      result += id2WorkspaceTarget[currentId]
      val currentDepth = depth[currentId]
      if (currentDepth == maxDepth) {
        continue
      }

      val successors = if (runtimeDependencies) {
        id2AllSuccessors[currentId]
      }
      else {
        id2CompileSuccessors[currentId]
      }
      for (succId in successors) {
        val target = id2WorkspaceTarget[succId]
        if (!condition(target.targetKey)) {
          continue
        }
        if (depth[succId] > currentDepth + 1) {
          depth[succId] = currentDepth + 1
          toVisit.addLast(succId)
        }
      }
    }

    return result
  }

  // MAYBE RC: avoid Sequence<...> as a return value?,
  //  there are ways to compute transitive closure faster
  //  but this approach is good enough
  override fun findAllTransitiveSuccessors(targetKey: WorkspaceTargetKey): Sequence<WorkspaceTarget> {
    val targetId = targetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID)
    if (targetId == INVALID_TARGET_ID) {
      return emptySequence()
    }
    return findAllTransitiveSuccessorIds(cache = id2TransitiveTargetCache, targetId = targetId)
      .asSequence()
      .map { transitiveSuccId -> id2WorkspaceTarget[transitiveSuccId] }
  }

  override fun findAllTransitiveSuccessorsWithoutRootTargets(targetKey: WorkspaceTargetKey): Sequence<WorkspaceTarget> {
    val targetId = targetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID)
    if (targetId == INVALID_TARGET_ID) {
      return emptySequence()
    }
    return id2AllSuccessors[targetId]
      .asSequence()
      .filterNot { depId -> rootTargetIds.contains(depId) }
      .flatMap { depId ->
        findAllTransitiveSuccessorIds(cache = id2TransitiveTargetCache, targetId = depId)
          .asSequence() + sequenceOf(depId)
      }
      .map { transitiveSuccId -> id2WorkspaceTarget[transitiveSuccId] }
  }

  private fun findAllTransitiveSuccessorIds(cache: AtomicReferenceArray<IntArray?>, targetId: Int): IntArray {
    val cached = cache.get(targetId)
    if (cached != null) {
      return cached
    }
    val visited = IntOpenHashSet()
    for (succId in id2CompileSuccessors[targetId]) {
      if (!visited.add(succId)) {
        continue
      }
      for (succDepId in findAllTransitiveSuccessorIds(cache, succId)) {
        visited.add(succDepId)
      }
    }
    val baked = visited.toIntArray()
    cache.compareAndSet(targetId, null, baked)
    return baked
  }

}

@ApiStatus.Internal
object WorkspaceTargetGraphBuilder {
  // RC: Generally speaking, `WorkspaceTargetGraph` shouldn't relay on rootTargets from BEP,
  //  instead workspace sync scope should be obtained using query, updating `rootTargets` incrementally
  //  and keeping track of it is hard problem to solve, that I don't want to solve - its not even worth looking into it
  //  for now when we're living in the world with monolithic sync pipeline passing entire set of
  //  `rootTargets` is fine, but for future change we shall consider removing it in order to provide easy
  //  to understand and simple incremental snapshot updating
  fun build(rootTargets: Set<Label>, targets: Collection<WorkspaceTarget>): WorkspaceTargetGraph {
    // with this trick, we have unique IDs for each WorkspaceTarget
    val id2WorkspaceTarget = targets.toTypedArray()
    val rootTargetIds = IntOpenHashSet()
    val targetKey2TargetId = Object2IntOpenHashMap<WorkspaceTargetKey>()
    for ((targetId, target) in id2WorkspaceTarget.withIndex()) {
      targetKey2TargetId.put(target.targetKey, targetId)
      if (target.targetKey.label in rootTargets) {
        rootTargetIds.add(targetId)
      }
    }

    for ((targetId, target) in id2WorkspaceTarget.withIndex()) {
      val generatorName = target.rawBuildTarget.generatorName ?: continue
      if (generatorName.isNotEmpty()) {
        val generatorTargetLabel = target.targetKey.label.assumeResolved()
          .copy(target = SingleTarget(generatorName))

        val generatorTargetKey = WorkspaceTargetKey(label = generatorTargetLabel, configuration = target.targetKey.configuration)
        val generatorTargetKeyWithConfiguration = WorkspaceTargetKey(label = generatorTargetLabel)
        val isGeneratorTargetInScope = sequenceOf(generatorTargetKey, generatorTargetKeyWithConfiguration)
          .map { targetKey2TargetId.getOrDefault(it, INVALID_TARGET_ID) }
          .any { it != INVALID_TARGET_ID }

        // Support macros that generate a target and refer to it by an alias, see https://youtrack.jetbrains.com/issue/BAZEL-3048
        // Aliases generated by macros can appear in rootTargets. But the aspect isn't run on aliases, so we can't get the target it refers to without a query.
        // However, every target generated by a macro has the original macro name in .generatorName.
        // We use that here to link the alias to its actual target in the graph, without an additional Bazel query invocation.
        if (generatorTargetLabel in rootTargets && !isGeneratorTargetInScope) {
          rootTargetIds.add(targetId)
        }
      }
    }

    fun buildSuccessorIds(predicate: (dependency: DependencyLabel) -> Boolean): Array<IntArray> {
      return Array(id2WorkspaceTarget.size) { targetId ->
        val target = id2WorkspaceTarget[targetId]
        target.rawBuildTarget.dependencies
          .filter { predicate(it) }
          .map { targetKey2TargetId.getOrDefault(it.targetKey, INVALID_TARGET_ID) }
          .filterNot { it == INVALID_TARGET_ID }
          .toIntArray()
      }
    }

    return WorkspaceTargetGraphImpl(
      rootTargetIds = rootTargetIds,
      targetKey2TargetId = targetKey2TargetId,
      id2WorkspaceTarget = id2WorkspaceTarget,
      id2CompileSuccessors = buildSuccessorIds {
        it.kind == DependencyLabelKind.COMPILE ||
          it.kind == DependencyLabelKind.COMPILE_NON_EXPORTED ||
          it.kind == DependencyLabelKind.EXPORTED_COMPILE_TIME
      },
      id2AllSuccessors = buildSuccessorIds { true },
    )
  }
}
