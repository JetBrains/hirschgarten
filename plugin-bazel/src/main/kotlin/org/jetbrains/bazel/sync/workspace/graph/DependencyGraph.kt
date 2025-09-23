package org.jetbrains.bazel.sync.workspace.graph

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import java.util.PriorityQueue

class DependencyGraph(
  private val rootTargets: Set<Label> = emptySet(),
  private val idToTargetInfo: Map<Label, BspTargetInfo.TargetInfo> = emptyMap(),
) {
  private val idToDirectDependenciesIds = mutableMapOf<Label, Set<Label>>()
  private val idToDirectCompileDependenciesIds = mutableMapOf<Label, Set<Label>>()
  private val idToReverseDependenciesIds = mutableMapOf<Label, HashSet<Label>>()
  private val idToLazyTransitiveDependencies: Map<Label, Lazy<Set<BspTargetInfo.TargetInfo>>>

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

  private fun createIdToLazyTransitiveDependenciesMap(
    idToTargetInfo: Map<Label, BspTargetInfo.TargetInfo>,
  ): Map<Label, Lazy<Set<BspTargetInfo.TargetInfo>>> =
    idToTargetInfo.mapValues { (_, targetInfo) ->
      calculateLazyTransitiveDependenciesForTarget(targetInfo)
    }

  private fun calculateLazyTransitiveDependenciesForTarget(targetInfo: BspTargetInfo.TargetInfo): Lazy<Set<BspTargetInfo.TargetInfo>> =
    lazy { calculateTransitiveDependenciesForTarget(targetInfo) }

  private fun calculateTransitiveDependenciesForTarget(targetInfo: BspTargetInfo.TargetInfo): Set<BspTargetInfo.TargetInfo> {
    val dependencies = getDependencies(targetInfo)
    val strictlyTransitiveDependencies = calculateStrictlyTransitiveDependencies(dependencies)
    val directDependencies = idsToTargetInfo(dependencies)
    return strictlyTransitiveDependencies + directDependencies
  }

  private fun calculateStrictlyTransitiveDependencies(dependencies: Set<Label>): Set<BspTargetInfo.TargetInfo> =
    dependencies
      .flatMap {
        idToLazyTransitiveDependencies[it]?.value.orEmpty()
      }.toSet()

  private fun idsToTargetInfo(dependencies: Collection<Label>): Set<BspTargetInfo.TargetInfo> =
    dependencies.mapNotNull(idToTargetInfo::get).toSet()

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

  data class TargetsAtDepth(val targets: Set<BspTargetInfo.TargetInfo>, val directDependencies: Set<BspTargetInfo.TargetInfo>)

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

  fun transitiveDependenciesWithoutRootTargets(targetId: Label): Set<BspTargetInfo.TargetInfo> =
    idToTargetInfo[targetId]
      ?.let(::getDependencies)
      .orEmpty()
      .filter(::isNotARootTarget)
      .flatMap(::collectTransitiveDependenciesAndAddTarget)
      .toSet()

  private fun getDependencies(target: BspTargetInfo.TargetInfo): Set<Label> = getDependencies(target.dependenciesList)

  private fun getCompileAndRuntimeDependencies(target: BspTargetInfo.TargetInfo): Pair<Set<Label>, Set<Label>> {
    val (compile, runtime) =
      target.dependenciesList
        .partition { it.dependencyTypeValue == BspTargetInfo.Dependency.DependencyType.COMPILE_VALUE }

    return getDependencies(compile) to getDependencies(runtime)
  }

  private fun getDependencies(dependencies: List<BspTargetInfo.Dependency>): Set<Label> =
    dependencies
      .map(BspTargetInfo.Dependency::getId)
      .map(Label::parse)
      .toSet()

  private fun isNotARootTarget(targetId: Label): Boolean = !rootTargets.contains(targetId)

  private fun collectTransitiveDependenciesAndAddTarget(targetId: Label): Set<BspTargetInfo.TargetInfo> {
    val target = idToTargetInfo[targetId]?.let(::setOf).orEmpty()
    val dependencies =
      idToLazyTransitiveDependencies[targetId]
        ?.let(::setOf)
        .orEmpty()
        .map(Lazy<Set<BspTargetInfo.TargetInfo>>::value)
        .flatten()
        .toSet()
    return dependencies + target
  }
}
