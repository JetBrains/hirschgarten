package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id

// TODO: migrate to WorkspaceTargetKey
@ApiStatus.Internal
object ExecutableTargetsComputer {
  fun calculateExecutableTargets(
    targets: Collection<BuildTarget>,
    labelToTargetInfo: Map<Label, BuildTarget>,
  ): Map<ResolvedLabel, List<Label>> {
    val targetDirectDependentsGraph = calculateDirectDependentsGraph(targets)
    val targetToTransitiveRevertedDependenciesCache = mutableMapOf<Label, Set<Label>>()
    val result = mutableMapOf<ResolvedLabel, MutableList<Label>>()
    targets
      .forEach { target ->
        val label = target.id
        val executables = calculateTransitivelyExecutableTargets(
          resultCache = targetToTransitiveRevertedDependenciesCache,
          targetDirectDependentsGraph = targetDirectDependentsGraph,
          labelToTargetInfo = labelToTargetInfo,
          target = label,
        )
        if (label is ResolvedLabel && executables.isNotEmpty()) {
          result[label] = executables.toMutableList()
        }
      }
    return result.mapValues { (_, executableTargets) -> executableTargets.sortedBy { it.toString() } }
  }

  private fun calculateDirectDependentsGraph(targets: Collection<BuildTarget>): Map<Label, Set<Label>> {
    val targetIdToDirectDependentIds = hashMapOf<Label, MutableSet<Label>>()
    for (targetInfo in targets) {
      val dependencies = targetInfo.dependencies
      for (dependency in dependencies) {
        targetIdToDirectDependentIds
          .computeIfAbsent(dependency.targetKey.label) { hashSetOf() }
          .add(targetInfo.id)
      }
    }
    return targetIdToDirectDependentIds
  }

  private fun calculateTransitivelyExecutableTargets(
    resultCache: MutableMap<Label, Set<Label>>,
    targetDirectDependentsGraph: Map<Label, Set<Label>>,
    target: Label,
    labelToTargetInfo: Map<Label, BuildTarget>,
  ): Set<Label> =
    resultCache.getOrPut(target) {
      val targetInfo = labelToTargetInfo[target]
      if (targetInfo?.kind?.isExecutable == true) {
        return@getOrPut setOf(target)
      }

      val directDependentIds = targetDirectDependentsGraph[target] ?: return@getOrPut emptySet()

      val executableTargetsFromSamePackage = directDependentIds.filter {
        it.packagePath == target.packagePath && labelToTargetInfo[it]?.kind?.isExecutable == true
      }
      if (executableTargetsFromSamePackage.isNotEmpty()) return@getOrPut executableTargetsFromSamePackage.toHashSet()

      return@getOrPut directDependentIds
        .asSequence()
        .flatMap { dependency ->
          calculateTransitivelyExecutableTargets(resultCache, targetDirectDependentsGraph, dependency, labelToTargetInfo)
        }.distinct()
        .take(MAX_EXECUTABLE_TARGET_IDS)
        .toHashSet()
    }
}

private const val MAX_EXECUTABLE_TARGET_IDS = 5
