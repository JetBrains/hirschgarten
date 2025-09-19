package org.jetbrains.bazel.server.dependencygraph

import org.jetbrains.bazel.info.BspTargetInfo.Dependency
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import java.util.PriorityQueue
import kotlin.math.min

class DependencyGraph(private val rootTargets: Set<Label> = emptySet(), private val idToTargetInfo: Map<Label, TargetInfo> = emptyMap()) {
  private val idToDirectDependenciesIds = mutableMapOf<Label, Set<Label>>()
  private val idToDirectCompileDependenciesIds = mutableMapOf<Label, Set<Label>>()
  private val idToReverseDependenciesIds = mutableMapOf<Label, HashSet<Label>>()
  private val idToLazyTransitiveDependencies: Map<Label, Lazy<Set<TargetInfo>>>

  init {

    idToTargetInfo.entries.forEach { (id, target) ->
      val (compile, runtime) = getCompileAndRuntimeDependencies(target)
      val dependencies = compile + runtime

      idToDirectDependenciesIds[id] = dependencies
      idToDirectCompileDependenciesIds[id] = compile

      dependencies.forEach { dep ->
        idToReverseDependenciesIds.computeIfAbsent(dep) { hashSetOf() }.add(id)
      }
    }
    idToLazyTransitiveDependencies = createIdToLazyTransitiveDependenciesMap(idToTargetInfo)
  }

  fun getReverseDependencies(id: Label): Set<Label> = idToReverseDependenciesIds[id].orEmpty()

  /**
   * Gets source files from reverse dependencies (umbrella targets) that contain Java/Kotlin sources.
   * This is useful for sharded libraries where each shard needs to see sources from umbrella targets
   * that depend on all shards.
   */
  fun getSourcesFromReverseDependencies(targetId: Label): Set<TargetInfo> {
    return getReverseDependencies(targetId)
      .mapNotNull { reverseDep -> idToTargetInfo[reverseDep] }
      .filter { it.hasJvmTargetInfo() }
      .toSet()
  }

  fun getTargetInfo(targetId: Label): TargetInfo? = idToTargetInfo[targetId]

  private fun createIdToLazyTransitiveDependenciesMap(idToTargetInfo: Map<Label, TargetInfo>): Map<Label, Lazy<Set<TargetInfo>>> =
    idToTargetInfo.mapValues { (_, targetInfo) ->
      calculateLazyTransitiveDependenciesForTarget(targetInfo)
    }

  private fun calculateLazyTransitiveDependenciesForTarget(targetInfo: TargetInfo): Lazy<Set<TargetInfo>> =
    lazy { calculateTransitiveDependenciesForTarget(targetInfo) }

  private fun calculateTransitiveDependenciesForTarget(targetInfo: TargetInfo): Set<TargetInfo> {
    val dependencies = getDependencies(targetInfo)
    val strictlyTransitiveDependencies = calculateStrictlyTransitiveDependencies(dependencies)
    val directDependencies = idsToTargetInfo(dependencies)
    return strictlyTransitiveDependencies + directDependencies
  }

  private fun calculateStrictlyTransitiveDependencies(dependencies: Set<Label>): Set<TargetInfo> =
    dependencies
      .flatMap {
        idToLazyTransitiveDependencies[it]?.value.orEmpty()
      }.toSet()

  private fun idsToTargetInfo(dependencies: Collection<Label>): Set<TargetInfo> = dependencies.mapNotNull(idToTargetInfo::get).toSet()

  private fun directDependenciesIds(targetIds: Collection<Label>) =
    targetIds
      .flatMap {
        idToDirectDependenciesIds[it].orEmpty()
      }.toSet()

  private fun directCompileDependenciesIds(targetIds: Collection<Label>) =
    targetIds
      .flatMap {
        idToDirectCompileDependenciesIds[it].orEmpty()
      }.toSet()

  data class TargetsAtDepth(val targets: Set<TargetInfo>, val directDependencies: Set<TargetInfo>)

  fun allTransitiveTargets(targets: Set<Label>): TargetsAtDepth =
    TargetsAtDepth(
      targets = idsToTargetInfo(targets) + calculateStrictlyTransitiveDependencies(targets),
      directDependencies = emptySet(),
    )

  fun allTargetsAtDepth(
    maxDepth: Int,
    targets: Set<Label>,
    isExternalTarget: (Label) -> Boolean = { false },
    targetSupportsStrictDeps: (Label) -> Boolean = { false },
    isWorkspaceTarget: (Label) -> Boolean = { true },
  ): TargetsAtDepth {
    if (maxDepth < 0) {
      return allTransitiveTargets(targets)
    }

    val depth = mutableMapOf<Label, Int>()
    val toVisit =
      PriorityQueue(
        Comparator<Label> { label1, label2 ->
          depth.getOrDefault(label1, 0).compareTo(depth.getOrDefault(label2, 0)).takeIf { it != 0 } ?: label1.compareTo(label2)
        },
      )
    val dependenciesOfNonStrictDepsTargets = mutableSetOf<Label>()

    var (currentTargets, nonWorkspaceTargets) =
      targets.partition { isWorkspaceTarget(it) }.let { (currentTargets, nonWorkspaceTargets) ->
        currentTargets.toSet() to nonWorkspaceTargets
      }
    currentTargets.forEach { target ->
      depth[target] = 0
      toVisit.add(target)
    }

    fun bfs(ignoreMaxDepth: Boolean = false) {
      while (toVisit.isNotEmpty()) {
        val current = toVisit.remove()
        val currentDepth = depth.getOrDefault(current, 0)
        if (!ignoreMaxDepth && currentDepth == maxDepth + 1) continue

        var isDependencyOfNonStrictDepsTarget = current in dependenciesOfNonStrictDepsTargets
        if (!isDependencyOfNonStrictDepsTarget && !targetSupportsStrictDeps(current)) {
          isDependencyOfNonStrictDepsTarget = true
          dependenciesOfNonStrictDepsTargets.add(current)
        }

        for (dep in idToDirectCompileDependenciesIds[current].orEmpty()) {
          val dependencyDepth = depth.getOrDefault(dep, Int.MAX_VALUE)
          val shouldUpdateDepth = currentDepth + 1 < dependencyDepth
          val shouldAddToDependenciesOfNonStrictTargets = isDependencyOfNonStrictDepsTarget && dep !in dependenciesOfNonStrictDepsTargets

          if (shouldUpdateDepth || shouldAddToDependenciesOfNonStrictTargets) {
            if (shouldUpdateDepth) {
              depth[dep] = currentDepth + 1
            }
            if (shouldAddToDependenciesOfNonStrictTargets) {
              dependenciesOfNonStrictDepsTargets.add(dep)
            }
            toVisit.add(dep)
          }
        }
      }
    }

    bfs()

    // Only traverse non-workspace targets if someone depends on them
    nonWorkspaceTargets.filter { it in depth }.forEach { target ->
      depth[target] = 0
      toVisit.add(target)
    }
    bfs()

    // Add all transitive external libraries for targets that don't support strict deps to avoid red code
    val directDependencies = depth.filter { (_, depth) -> depth > maxDepth }.map { (target, _) -> target }
    toVisit.addAll(directDependencies.filter { it in dependenciesOfNonStrictDepsTargets }.filter { isExternalTarget(it) })
    bfs(ignoreMaxDepth = true)

    val (finalTargets, finalDirectDependencies) = depth.entries.partition { (_, depth) -> depth <= maxDepth }

    return TargetsAtDepth(
      targets = idsToTargetInfo(finalTargets.map { (target, _) -> target }),
      directDependencies = idsToTargetInfo(finalDirectDependencies.map { (target, _) -> target }),
    )
  }

  fun transitiveDependenciesWithoutRootTargets(targetId: Label): Set<TargetInfo> =
    idToTargetInfo[targetId]
      ?.let(::getDependencies)
      .orEmpty()
      .filter(::isNotARootTarget)
      .flatMap(::collectTransitiveDependenciesAndAddTarget)
      .toSet()

  private fun getDependencies(target: TargetInfo): Set<Label> = getDependencies(target.dependenciesList)

  private fun getCompileAndRuntimeDependencies(target: TargetInfo): Pair<Set<Label>, Set<Label>> {
    val (compile, runtime) =
      target.dependenciesList
        .partition { it.dependencyTypeValue == Dependency.DependencyType.COMPILE_VALUE }

    return getDependencies(compile) to getDependencies(runtime)
  }

  private fun getDependencies(dependencies: List<Dependency>): Set<Label> =
    dependencies
      .map(Dependency::getId)
      .map(Label::parse)
      .toSet()

  private fun isNotARootTarget(targetId: Label): Boolean = !rootTargets.contains(targetId)

  private fun collectTransitiveDependenciesAndAddTarget(targetId: Label): Set<TargetInfo> {
    val target = idToTargetInfo[targetId]?.let(::setOf).orEmpty()
    val dependencies =
      idToLazyTransitiveDependencies[targetId]
        ?.let(::setOf)
        .orEmpty()
        .map(Lazy<Set<TargetInfo>>::value)
        .flatten()
        .toSet()
    return dependencies + target
  }
}
