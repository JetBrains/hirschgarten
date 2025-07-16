package org.jetbrains.bazel.server.dependencygraph

import org.jetbrains.bazel.info.Dependency
import org.jetbrains.bazel.info.DependencyType
import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import java.util.PriorityQueue

class DependencyGraph(
  private val rootTargets: Set<CanonicalLabel> = emptySet(),
  private val idToTargetInfo: Map<CanonicalLabel, TargetInfo> = emptyMap(),
) {
  private val idToDirectDependenciesIds = mutableMapOf<CanonicalLabel, Set<CanonicalLabel>>()
  private val idToDirectCompileDependenciesIds = mutableMapOf<CanonicalLabel, Set<CanonicalLabel>>()
  private val idToReverseDependenciesIds = mutableMapOf<CanonicalLabel, HashSet<CanonicalLabel>>()
  private val idToLazyTransitiveDependencies: Map<CanonicalLabel, Lazy<Set<TargetInfo>>>

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

  fun getReverseDependencies(id: CanonicalLabel): Set<CanonicalLabel> = idToReverseDependenciesIds[id].orEmpty()

  private fun createIdToLazyTransitiveDependenciesMap(
    idToTargetInfo: Map<CanonicalLabel, TargetInfo>,
  ): Map<CanonicalLabel, Lazy<Set<TargetInfo>>> =
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

  private fun calculateStrictlyTransitiveDependencies(dependencies: Set<CanonicalLabel>): Set<TargetInfo> =
    dependencies
      .flatMap {
        idToLazyTransitiveDependencies[it]?.value.orEmpty()
      }.toSet()

  private fun idsToTargetInfo(dependencies: Collection<CanonicalLabel>): Set<TargetInfo> =
    dependencies.mapNotNull(idToTargetInfo::get).toSet()

  private fun directDependenciesIds(targetIds: Collection<CanonicalLabel>) =
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

  fun allTargetsAtDepth(
    maxDepth: Int,
    targets: Set<CanonicalLabel>,
    isExternalTarget: (CanonicalLabel) -> Boolean = { false },
    targetSupportsStrictDeps: (CanonicalLabel) -> Boolean = { false },
    isWorkspaceTarget: (CanonicalLabel) -> Boolean = { true },
  ): TargetsAtDepth {
    if (maxDepth < 0) {
      return TargetsAtDepth(
        targets = idsToTargetInfo(targets) + calculateStrictlyTransitiveDependencies(targets),
        directDependencies = emptySet(),
      )
    }

    val depth = mutableMapOf<CanonicalLabel, Int>()
    val toVisit =
      PriorityQueue(
        Comparator<CanonicalLabel> { label1, label2 ->
          depth.getOrDefault(label1, 0).compareTo(depth.getOrDefault(label2, 0)).takeIf { it != 0 } ?: label1.compareTo(label2)
        },
      )
    val dependenciesOfNonStrictDepsTargets = mutableSetOf<CanonicalLabel>()

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
        val targetSupportsStrictDeps = targetSupportsStrictDeps(current)
        for (dependency in idToDirectCompileDependenciesIds[current].orEmpty()) {
          val dependencyDepth = depth.getOrDefault(dependency, Int.MAX_VALUE)
          if (currentDepth + 1 < dependencyDepth) {
            depth[dependency] = currentDepth + 1
            toVisit.add(dependency)
            if (!targetSupportsStrictDeps) {
              dependenciesOfNonStrictDepsTargets.add(dependency)
            }
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

  fun transitiveDependenciesWithoutRootTargets(targetId: CanonicalLabel): Set<TargetInfo> =
    idToTargetInfo[targetId]
      ?.let(::getDependencies)
      .orEmpty()
      .filter(::isNotARootTarget)
      .flatMap(::collectTransitiveDependenciesAndAddTarget)
      .toSet()

  private fun getDependencies(target: TargetInfo): Set<CanonicalLabel> = target.dependencies.map { it.id }.toSet()

  private fun getCompileAndRuntimeDependencies(target: TargetInfo): Pair<Set<CanonicalLabel>, Set<CanonicalLabel>> {
    val (compile, runtime) =
      target.dependencies
        .partition { it.dependencyType == DependencyType.COMPILE }

    return compile.map { it.id }.toSet() to runtime.map { it.id }.toSet()
  }

  private fun isNotARootTarget(targetId: CanonicalLabel): Boolean = !rootTargets.contains(targetId)

  private fun collectTransitiveDependenciesAndAddTarget(targetId: CanonicalLabel): Set<TargetInfo> {
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
