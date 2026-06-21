package org.jetbrains.bazel.sync.workspace.snapshot

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2ObjectMap
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
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
   * @param strict Match [targetKey] exactly, when false try matching without propagated aspects [WorkspaceTargetKey.aspectIds]
   *
   * @return Matched target
   */
  fun findTargetByKey(targetKey: WorkspaceTargetKey, strict: Boolean = false): WorkspaceTarget?

  /**
   * Find all [WorkspaceTarget] that lossy match [targetKey], match only by label and configuration
   *
   * @param targetKey Specific [WorkspaceTargetKey]
   *
   * @return All matching targets
   */
  fun findTargetsLossy(targetKey: WorkspaceTargetKey): Sequence<WorkspaceTarget>

  /**
   * Walk graph and find all targets until depth [maxDepth]
   *
   * @param maxDepth Max walk depth
   * @param runtimeDependencies Do include all runtime dependencies
   * @param useRelaxedDependencyExpansion Ignore propagated aspect when traversing through dependencies
   * @param condition Include targets that satisfy this condition
   *
   * @return Target list in topological order, according to [maxDepth] setting
   */
  fun findAllTargetsAtDepth(
    maxDepth: Int,
    runtimeDependencies: Boolean = maxDepth < 0,
    useRelaxedDependencyExpansion: Boolean = false,
    condition: (key: WorkspaceTargetKey) -> Boolean = { true },
  ): List<WorkspaceTarget>

  /**
   * Find all transitive successors to [targetKey]
   *
   * @param targetKey Target key
   * @param useRelaxedDependencyExpansion Ignore propagated aspect when traversing through dependencies
   *
   * @return Sequence of all transitive successors of [targetKey]
   */
  fun findAllTransitiveSuccessors(
    targetKey: WorkspaceTargetKey,
    useRelaxedDependencyExpansion: Boolean = false,
  ): Sequence<WorkspaceTarget>

  /**
   * Find all transitive successors to [targetKey] excluding traversal over root targets
   *
   * @param targetKey Target key
   * @param useRelaxedDependencyExpansion Ignore propagated aspect when traversing through dependencies
   *
   * @return Sequence of all transitive successors of [targetKey] exclusing root targets
   */
  fun findAllTransitiveSuccessorsWithoutRootTargets(
    targetKey: WorkspaceTargetKey,
    useRelaxedDependencyExpansion: Boolean = false,
  ): Sequence<WorkspaceTarget>

}

internal const val INVALID_TARGET_ID: Int = -1

internal data class LabelConfigKey(val label: Label, val configuration: WorkspaceConfigurationId)

/**
 * Low memory footprint, immutable target graph representation
 */
internal class WorkspaceTargetGraphImpl internal constructor(
  private val rootTargetIds: IntSet,
  private val targetKey2TargetId: Object2IntMap<WorkspaceTargetKey>,
  private val labelConfig2TargetIds: Object2ObjectMap<LabelConfigKey, IntArray>,
  private val id2WorkspaceTarget: Array<WorkspaceTarget>,
  private val id2CompileSuccessors: Array<IntArray>,
  private val id2AllSuccessors: Array<IntArray>,
  private val id2RelaxedCompileSuccessors: Array<IntArray>,
  private val id2RelaxedAllSuccessors: Array<IntArray>,
) : WorkspaceTargetGraph {
  private val id2TransitiveTargetCache: AtomicReferenceArray<IntArray?> = AtomicReferenceArray(targetKey2TargetId.size)

  override val allTargets: Array<WorkspaceTarget> = id2WorkspaceTarget

  override fun findTargetByKey(targetKey: WorkspaceTargetKey, strict: Boolean): WorkspaceTarget? {
    val targetId = targetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID)
    if (targetId != INVALID_TARGET_ID) {
      return id2WorkspaceTarget[targetId]
    }
    if (strict) {
      return null
    }
    // shadow graph fallback: pick first matching
    val canonical = labelConfig2TargetIds[LabelConfigKey(targetKey.label, targetKey.configuration)]?.firstOrNull() ?: INVALID_TARGET_ID
    return if (canonical == INVALID_TARGET_ID) {
      null
    }
    else {
      id2WorkspaceTarget[canonical]
    }
  }

  override fun findTargetsLossy(targetKey: WorkspaceTargetKey): Sequence<WorkspaceTarget> =
    (labelConfig2TargetIds[LabelConfigKey(label = targetKey.label, configuration = targetKey.configuration)] ?: intArrayOf())
      .asSequence()
      .map { id2WorkspaceTarget[it] }

  override fun findAllTargetsAtDepth(
    maxDepth: Int,
    runtimeDependencies: Boolean,
    useRelaxedDependencyExpansion: Boolean,
    condition: (key: WorkspaceTargetKey) -> Boolean,
  ): List<WorkspaceTarget> {
    val depth = IntArray(id2WorkspaceTarget.size) { Int.MAX_VALUE }

    // TODO: use primitive collection here
    val toVisit = ArrayDeque<Int>()
    val result = mutableListOf<WorkspaceTarget>()

    val compileSuccessors = if (useRelaxedDependencyExpansion) {
      id2RelaxedCompileSuccessors
    }
    else {
      id2CompileSuccessors
    }
    val allSuccessors = if (useRelaxedDependencyExpansion) {
      id2RelaxedAllSuccessors
    }
    else {
      id2AllSuccessors
    }

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

      val successors = if (runtimeDependencies) allSuccessors[currentId] else compileSuccessors[currentId]
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
  override fun findAllTransitiveSuccessors(
    targetKey: WorkspaceTargetKey,
    useRelaxedDependencyExpansion: Boolean,
  ): Sequence<WorkspaceTarget> {
    val targetId = targetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID)
    if (targetId == INVALID_TARGET_ID) {
      return emptySequence()
    }
    return computeTransitiveSuccessorIds(targetId, useRelaxedDependencyExpansion)
      .asSequence()
      .map { transitiveSuccId -> id2WorkspaceTarget[transitiveSuccId] }
  }

  override fun findAllTransitiveSuccessorsWithoutRootTargets(
    targetKey: WorkspaceTargetKey,
    useRelaxedDependencyExpansion: Boolean,
  ): Sequence<WorkspaceTarget> {
    val targetId = targetKey2TargetId.getOrDefault(targetKey, INVALID_TARGET_ID)
    if (targetId == INVALID_TARGET_ID) {
      return emptySequence()
    }
    val allSuccessors = if (useRelaxedDependencyExpansion) id2RelaxedAllSuccessors else id2AllSuccessors
    return allSuccessors[targetId]
      .asSequence()
      .filterNot { depId -> rootTargetIds.contains(depId) }
      .flatMap { depId ->
        computeTransitiveSuccessorIds(depId, useRelaxedDependencyExpansion)
          .asSequence() + sequenceOf(depId)
      }
      .map { transitiveSuccId -> id2WorkspaceTarget[transitiveSuccId] }
  }

  private fun computeTransitiveSuccessorIds(targetId: Int, useRelaxedDependencyExpansion: Boolean): IntArray =
    if (useRelaxedDependencyExpansion) {
      // TODO: cache relaxed transitive closures if profiling shows it matters
      computeTransitiveSuccessorSlow(targetId, id2RelaxedCompileSuccessors)
    }
    else {
      findAllTransitiveSuccessorIds(cache = id2TransitiveTargetCache, targetId = targetId)
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

  private fun computeTransitiveSuccessorSlow(targetId: Int, adjacency: Array<IntArray>): IntArray {
    val visited = IntOpenHashSet()
    val stack = ArrayDeque<Int>()
    stack.addLast(targetId)
    while (stack.isNotEmpty()) {
      val currentId = stack.removeLast()
      for (succId in adjacency[currentId]) {
        if (visited.add(succId)) {
          stack.addLast(succId)
        }
      }
    }
    return visited.toIntArray()
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
  fun build(rootTargets: Set<WorkspaceTargetKey>, targets: Collection<WorkspaceTarget>): WorkspaceTargetGraph {
    // with this trick, we have unique IDs for each WorkspaceTarget
    val id2WorkspaceTarget = targets.toTypedArray()
    val rootTargetIds = IntOpenHashSet()
    val targetKey2TargetId = Object2IntOpenHashMap<WorkspaceTargetKey>()

    val labelConfig2TargetIdList = HashMap<LabelConfigKey, IntArrayList>()
    for ((targetId, target) in id2WorkspaceTarget.withIndex()) {
      targetKey2TargetId.put(target.targetKey, targetId)
      val smallKey = LabelConfigKey(target.targetKey.label, target.targetKey.configuration)
      labelConfig2TargetIdList.getOrPut(smallKey) { IntArrayList() }.add(targetId)
      if (target.targetKey in rootTargets) {
        rootTargetIds.add(targetId)
      }
    }

    @Suppress("SSBasedInspection")
    val labelConfig2TargetIds = Object2ObjectOpenHashMap<LabelConfigKey, IntArray>(labelConfig2TargetIdList.size)
    for ((key, ids) in labelConfig2TargetIdList) {
      labelConfig2TargetIds[key] = ids.toIntArray()
    }

    val rootLabels: Set<Label> = rootTargets.map { it.label }.toHashSet()
    for ((targetId, target) in id2WorkspaceTarget.withIndex()) {
      val generatorName = target.rawBuildTarget.generatorName ?: continue
      if (generatorName.isNotEmpty()) {
        val generatorTargetLabel = target.targetKey.label.assumeResolved()
          .copy(target = SingleTarget(generatorName))

        // try the macro/alias key with the current target's `aspectIds` propagated first,
        // then the configuration-only key, then the label-only key.
        val generatorTargetKey = WorkspaceTargetKey(label = generatorTargetLabel, configuration = target.targetKey.configuration)
        val generatorTargetKeyWithAspect =
          WorkspaceTargetKey(
            label = generatorTargetLabel,
            configuration = target.targetKey.configuration,
            aspectIds = target.targetKey.aspectIds,
          )
        val generatorTargetKeyWithConfiguration = WorkspaceTargetKey(label = generatorTargetLabel)
        val isGeneratorTargetInScope = sequenceOf(generatorTargetKeyWithAspect, generatorTargetKey, generatorTargetKeyWithConfiguration)
          .map { targetKey2TargetId.getOrDefault(it, INVALID_TARGET_ID) }
          .any { it != INVALID_TARGET_ID }

        // Support macros that generate a target and refer to it by an alias, see https://youtrack.jetbrains.com/issue/BAZEL-3048
        // Aliases generated by macros can appear in rootTargets. But the aspect isn't run on aliases, so we can't get the target it refers to without a query.
        // However, every target generated by a macro has the original macro name in .generatorName.
        // We use that here to link the alias to its actual target in the graph, without an additional Bazel query invocation.
        if (generatorTargetKey.label in rootLabels && !isGeneratorTargetInScope) {
          rootTargetIds.add(targetId)
        }
      }
    }

    fun resolveDepId(parent: WorkspaceTarget, dep: DependencyLabel): Int {
      val exact = targetKey2TargetId.getOrDefault(dep.targetKey, INVALID_TARGET_ID)
      if (exact != INVALID_TARGET_ID) {
        return exact
      }
      // shadow graph stuff: try key with parent aspects
      if (parent.targetKey.aspectIds == WorkspaceAspectIds.EMPTY) {
        return INVALID_TARGET_ID
      }
      val propagated = dep.targetKey.copy(aspectIds = parent.targetKey.aspectIds)
      return targetKey2TargetId.getOrDefault(propagated, INVALID_TARGET_ID)
    }

    fun buildSuccessorIds(predicate: (dependency: DependencyLabel) -> Boolean): Array<IntArray> {
      return Array(id2WorkspaceTarget.size) { targetId ->
        val target = id2WorkspaceTarget[targetId]
        target.rawBuildTarget.dependencies
          .filter { predicate(it) }
          .map { resolveDepId(target, it) }
          .filterNot { it == INVALID_TARGET_ID }
          .toIntArray()
      }
    }

    fun resolveDepIdsRelaxed(dep: DependencyLabel): IntArray =
      labelConfig2TargetIds[LabelConfigKey(dep.targetKey.label, dep.targetKey.configuration)] ?: IntArray(0)

    fun buildRelaxedSuccessorIds(predicate: (dependency: DependencyLabel) -> Boolean): Array<IntArray> {
      return Array(id2WorkspaceTarget.size) { targetId ->
        val target = id2WorkspaceTarget[targetId]
        val acc = IntOpenHashSet()
        for (dep in target.rawBuildTarget.dependencies) {
          if (!predicate(dep)) continue
          for (id in resolveDepIdsRelaxed(dep)) acc.add(id)
        }
        acc.toIntArray()
      }
    }

    val compilePredicate: (DependencyLabel) -> Boolean =
      { it.kind == DependencyLabelKind.COMPILE || it.kind == DependencyLabelKind.EXPORTED_COMPILE_TIME }
    val allPredicate: (DependencyLabel) -> Boolean = { true }

    return WorkspaceTargetGraphImpl(
      rootTargetIds = rootTargetIds,
      targetKey2TargetId = targetKey2TargetId,
      labelConfig2TargetIds = labelConfig2TargetIds,
      id2WorkspaceTarget = id2WorkspaceTarget,
      id2CompileSuccessors = buildSuccessorIds(compilePredicate),
      id2AllSuccessors = buildSuccessorIds(allPredicate),
      id2RelaxedCompileSuccessors = buildRelaxedSuccessorIds(compilePredicate),
      id2RelaxedAllSuccessors = buildRelaxedSuccessorIds(allPredicate),
    )
  }
}
