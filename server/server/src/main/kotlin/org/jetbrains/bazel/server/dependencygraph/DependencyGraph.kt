package org.jetbrains.bazel.server.dependencygraph

import org.jetbrains.bazel.info.BspTargetInfo.Dependency
import org.jetbrains.bazel.info.BspTargetInfo.TargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.label.label

class DependencyGraph(private val rootTargets: Set<Label> = emptySet(), private val idToTargetInfo: Map<Label, TargetInfo> = emptyMap()) {
  private val idToDirectDependenciesIds = mutableMapOf<Label, Set<Label>>()
  private val idToReverseDependenciesIds = mutableMapOf<Label, HashSet<Label>>()
  private val idToLazyTransitiveDependencies: Map<Label, Lazy<Set<TargetInfo>>>

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

  private fun idsToTargetInfo(dependencies: Set<Label>): Set<TargetInfo> = dependencies.mapNotNull(idToTargetInfo::get).toSet()

  private fun directDependenciesIds(targetIds: Set<Label>) =
    targetIds
      .flatMap {
        idToDirectDependenciesIds[it].orEmpty()
      }.toSet()

  data class TargetsAtDepth(val targets: Set<TargetInfo>, val directDependencies: Set<TargetInfo>)

  fun allTargetsAtDepth(depth: Int, targets: Set<Label>): TargetsAtDepth {
    if (depth < 0) {
      return TargetsAtDepth(
        targets = idsToTargetInfo(targets) + calculateStrictlyTransitiveDependencies(targets),
        directDependencies = emptySet(),
      )
    }

    var currentDepth = depth
    val searched = mutableSetOf<Label>()
    var currentTargets = targets

    while (currentDepth >= 0) {
      searched.addAll(currentTargets)
      currentTargets = directDependenciesIds(currentTargets).filterTo(mutableSetOf()) { it !in searched }
      currentDepth--
    }

    return TargetsAtDepth(
      targets = idsToTargetInfo(searched),
      directDependencies = idsToTargetInfo(currentTargets),
    )
  }

  fun transitiveDependenciesWithoutRootTargets(targetId: Label): Set<TargetInfo> =
    idToTargetInfo[targetId]
      ?.let(::getDependencies)
      .orEmpty()
      .filter(::isNotARootTarget)
      .flatMap(::collectTransitiveDependenciesAndAddTarget)
      .toSet()

  private fun getDependencies(target: TargetInfo): Set<Label> =
    target.dependenciesList
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

  fun filterUsedLibraries(libraries: Map<Label, TargetInfo>, targets: Sequence<TargetInfo>): Map<Label, TargetInfo> {
    val visited = hashSetOf<Label>()
    val queue = ArrayDeque<Label>()
    targets.map { it.label() }.forEach {
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
