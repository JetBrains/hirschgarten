package org.jetbrains.bazel.sync.workspace.graph

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import kotlin.collections.filter
import kotlin.collections.orEmpty

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

  fun allTargetsAtDepth(
    depth: Int,
    targets: Set<Label>,
    isExternalTarget: (Label) -> Boolean = { false },
    targetSupportsStrictDeps: (Label) -> Boolean = { false },
    isWorkspaceTarget: (Label) -> Boolean = { true },
  ): TargetsAtDepth {
    if (depth < 0) {
      return TargetsAtDepth(
        targets = idsToTargetInfo(targets) + calculateStrictlyTransitiveDependencies(targets),
        directDependencies = emptySet(),
      )
    }

    val visited = mutableSetOf<Label>()

    fun Collection<Label>.filterNotVisited(): Set<Label> = filterTo(mutableSetOf()) { it !in visited }

    var currentDepth = depth
    var currentTargets = targets.filter { isWorkspaceTarget(it) }.toSet()

    while (currentDepth > 0) {
      visited.addAll(currentTargets)
      currentTargets = directDependenciesIds(currentTargets).filterNotVisited()
      currentDepth--
    }

    // Handle last level separately
    visited.addAll(currentTargets)
    val (targetsWithStrictDeps, targetsWithNonStrictDeps) = currentTargets.partition { targetSupportsStrictDeps(it) }
    val directDependenciesOfNonStrictDeps = directCompileDependenciesIds(targetsWithNonStrictDeps).filterNotVisited()
    val directDependenciesOfStrictDeps = directCompileDependenciesIds(targetsWithStrictDeps).filterNotVisited()

    // Add all transitive libraries for targets that don't support strict deps to avoid red code
    val (externalTargets, internalTargets) = directDependenciesOfNonStrictDeps.partition { isExternalTarget(it) }
    val toVisit = ArrayDeque(externalTargets)
    visited.addAll(toVisit)
    while (toVisit.isNotEmpty()) {
      val current = toVisit.removeFirst()
      val directDependencies = idToDirectDependenciesIds[current].orEmpty()
      for (dependency in directDependencies) {
        if (dependency !in visited) {
          visited.add(dependency)
          toVisit.addLast(dependency)
        }
      }
    }

    val directDependencies = internalTargets.filterNotVisited() + directDependenciesOfStrictDeps.filterNotVisited()

    return TargetsAtDepth(
      targets = idsToTargetInfo(visited),
      directDependencies = idsToTargetInfo(directDependencies),
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
