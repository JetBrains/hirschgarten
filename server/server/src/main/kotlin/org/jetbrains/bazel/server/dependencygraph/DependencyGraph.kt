package org.jetbrains.bazel.server.dependencygraph

import org.jetbrains.bazel.info.TargetInfo
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label

class DependencyGraph(private val rootTargets: Set<CanonicalLabel> = emptySet(), private val idToTargetInfo: Map<CanonicalLabel, TargetInfo> = emptyMap()) {
  private val idToDirectDependenciesIds = mutableMapOf<CanonicalLabel, Set<CanonicalLabel>>()
  private val idToReverseDependenciesIds = mutableMapOf<CanonicalLabel, HashSet<CanonicalLabel>>()
  private val idToLazyTransitiveDependencies: Map<CanonicalLabel, Lazy<Set<TargetInfo>>>

  init {

    idToTargetInfo.entries.forEach { (id, target) ->
      val dependencies = getDependencies(target)

      idToDirectDependenciesIds[id] = dependencies

      dependencies.forEach { dep ->
        idToReverseDependenciesIds.computeIfAbsent(dep) { hashSetOf() }.add(id)
      }
    }
    idToLazyTransitiveDependencies = createIdToLazyTransitiveDependenciesMap(idToTargetInfo)
  }

  fun getReverseDependencies(id: CanonicalLabel): Set<CanonicalLabel> = idToReverseDependenciesIds[id].orEmpty()

  private fun createIdToLazyTransitiveDependenciesMap(idToTargetInfo: Map<CanonicalLabel, TargetInfo>): Map<CanonicalLabel, Lazy<Set<TargetInfo>>> =
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

  private fun idsToTargetInfo(dependencies: Collection<CanonicalLabel>): Set<TargetInfo> = dependencies.mapNotNull(idToTargetInfo::get).toSet()

  private fun directDependenciesIds(targetIds: Set<CanonicalLabel>) =
    targetIds
      .flatMap {
        idToDirectDependenciesIds[it].orEmpty()
      }.toSet()

  data class TargetsAtDepth(val targets: Set<TargetInfo>, val directDependencies: Set<TargetInfo>)

  fun allTargetsAtDepth(
    depth: Int,
    targets: Set<CanonicalLabel>,
    isExternalTarget: (CanonicalLabel) -> Boolean,
  ): TargetsAtDepth {
    if (depth < 0) {
      return TargetsAtDepth(
        targets = idsToTargetInfo(targets) + calculateStrictlyTransitiveDependencies(targets),
        directDependencies = emptySet(),
      )
    }

    var currentDepth = depth
    val searched = mutableSetOf<CanonicalLabel>()
    var currentTargets = targets

    while (currentDepth >= 0) {
      searched.addAll(currentTargets)
      currentTargets = directDependenciesIds(currentTargets).filterTo(mutableSetOf()) { it !in searched }
      currentDepth--
    }

    // Add all transitive dependencies for external targets
    val (externalTargets, directInternalTargets) = currentTargets.partition { isExternalTarget(it) }
    val toVisit = ArrayDeque(externalTargets)
    searched.addAll(toVisit)
    while (toVisit.isNotEmpty()) {
      val current = toVisit.removeFirst()
      val directDependencies = idToDirectDependenciesIds[current].orEmpty()
      for (dependency in directDependencies) {
        if (dependency !in searched) {
          searched.add(dependency)
          toVisit.addLast(dependency)
        }
      }
    }

    return TargetsAtDepth(
      targets = idsToTargetInfo(searched),
      directDependencies = idsToTargetInfo(directInternalTargets),
    )
  }

  fun transitiveDependenciesWithoutRootTargets(targetId: CanonicalLabel): Set<TargetInfo> =
    idToTargetInfo[targetId]
      ?.let(::getDependencies)
      .orEmpty()
      .filter(::isNotARootTarget)
      .flatMap(::collectTransitiveDependenciesAndAddTarget)
      .toSet()

  private fun getDependencies(target: TargetInfo): Set<CanonicalLabel> =
    target.dependencies.map {it.id}
      .toSet()

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

  fun filterUsedLibraries(libraries: Map<CanonicalLabel, TargetInfo>, targets: Sequence<TargetInfo>): Map<CanonicalLabel, TargetInfo> {
    val visited = hashSetOf<CanonicalLabel>()
    val queue = ArrayDeque<CanonicalLabel>()
    targets.map { it.id }.forEach {
      queue.addLast(it)
      visited.add(it)
    }
    while (queue.isNotEmpty()) {
      val label = queue.removeFirst()
      val dependencies = idToDirectDependenciesIds[label] ?: continue
      for (dependency in dependencies) {
        if (!visited.add(dependency)) continue
        queue.addLast(dependency)
      }
    }
    return libraries.filterKeys { it in visited }
  }
}
