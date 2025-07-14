package org.jetbrains.bazel.server.dependencygraph

import org.jetbrains.bazel.info.BspTargetInfo.Dependency
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label

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
