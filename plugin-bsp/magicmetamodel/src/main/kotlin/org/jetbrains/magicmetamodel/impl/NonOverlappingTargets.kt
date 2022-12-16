package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace

/**
 * ## Conflicting Targets Problem
 *
 * The main purpose of the NonOverlappingTargets class is to resolve a mismatch between BSP model and IntelliJ model
 *
 * Roughly speaking, BSP allows targets to share source files. For example, let's assume a project contains
 * two libraries named libfoo and libbar, and three files named a.java, b.java and c.java.
 *
 * In BSP, it is perfectly valid for libfoo to contain a.java and b.java, while libbar contains b.java and c.java.
 * In this case we say that libfoo and libbar are *conflicting* targets.
 *
 * Unfortunately, in IntelliJ model, the relationship between file and module is one-to-one, so if we want to import
 * each BSP target as a separate module, we need to ignore some targets, to make sure there are no conflicting ones.
 *
 *
 * ## NonOverlappingTargets resolver
 * The `NonOverlappingTargets` class tries to run a heuristic that will drop some targets and return a result that is
 * a subset of all targets in which any element is not conflicting with any other.
 *
 * It takes two parameters,
 * - `allTargets` - a set of targets we are trying to import
 * - `conflictGraph` - a graph defining which target conflicts with each other. It is just a map, where the value is
 * a set of targets that conflict with the key.
 *
 * ## The algorithm
 * The algorithm works in that way, it iterates as long as the conflict graph is not empty. During each iteration, it
 * takes all the targets that has no conflicts, and adds them to the result list. We name them the *accepted* ones.
 * The targets (if there are any) are therefore removed from the conflict graph.
 * After that, it chooses a `worst` target (described below) and deletes it from the conflict graph, too.
 *
 * ## The worst target
 * When conflicting targets exist, there is no objective way to find out which one should be removed. That's why we need
 * to apply arbitrary heuristic. The heurisic we currently use is defined in `chooseWorstConflict` method and is defined
 * as follows
 * 1. Out of all targets that still has not been *accepted*, we find ones that are dependent on the *accepted* ones.
 * We call them *dependers*. We just find a target conflicting with any of the *dependers*.
 * 2. If step one did not provide any result, we just take a target that has the biggest number of conflicts
 *
 */
public object NonOverlappingTargets {
  private val log = logger<NonOverlappingTargets>()

  public operator fun invoke(
    allTargets: Set<BuildTarget>,
    conflictGraph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
  ): Set<BuildTargetIdentifier> {
    log.trace { "Calculating non overlapping targets for $allTargets..." }
    val invertedDependencyMap = getInvertedDependencyMap(allTargets)
    val fullConflictGraph = allTargets.associate { it.id to emptySet<BuildTargetIdentifier>() } + conflictGraph
    return extractNonConflictingTargets(ConflictGraph(fullConflictGraph.toMutableMap()), invertedDependencyMap)
  }

  private fun extractNonConflictingTargets(
    conflictGraph: ConflictGraph,
    invertedDependencyMap: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
  ): Set<BuildTargetIdentifier> {
    val elementsToTake = mutableSetOf<BuildTargetIdentifier>()
    val availableDependers = mutableSetOf<BuildTargetIdentifier>()

    while (conflictGraph.isNotEmpty()) {
      log.trace("Removing elements from conflict graph. Elements left: ${conflictGraph.connectedNodes.size}")
      elementsToTake.addAll(conflictGraph.isolatedNodes)
      availableDependers.addAll(conflictGraph.isolatedNodes.flatMap { invertedDependencyMap[it].orEmpty() })

      val currentConflict = chooseWorstConflict(conflictGraph, availableDependers)

      val elementsToRemove = elementsToTake + setOfNotNull(currentConflict)
      conflictGraph.removeAll(elementsToRemove)
      availableDependers.removeAll(elementsToRemove)
    }
    return elementsToTake
  }

  private fun mostConflictingTargets(conflictGraph: ConflictGraph): BuildTargetIdentifier? {
    val comparatorByOverlaps = Comparator.comparingInt<BuildTargetIdentifier> {
      conflictGraph.conflictMap[it]?.size ?: 0
    }
    return conflictGraph.connectedNodes.maxWithOrNull(comparatorByOverlaps)
  }

  private fun conflictingWithDependers(conflictGraph: ConflictGraph, availableDependers: MutableSet<BuildTargetIdentifier>): BuildTargetIdentifier? =
    availableDependers.flatMap { conflictGraph.conflictMap[it].orEmpty() }.firstOrNull()

  private fun chooseWorstConflict(conflictGraph: ConflictGraph,
                                  dependers: MutableSet<BuildTargetIdentifier>): BuildTargetIdentifier? =
    conflictingWithDependers(conflictGraph, dependers)
      ?: mostConflictingTargets(conflictGraph)

  private fun getInvertedDependencyMap(allTargets: Set<BuildTarget>): Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>> =
    allTargets.fold(emptyMap()) { acc, target ->
      val newEntries = target.dependencies.map { it to target.id }
      newEntries.fold(acc) { smallAcc, entry ->
        smallAcc + (entry.first to (smallAcc[entry.first].orEmpty() + entry.second))
      }
    }
}

public class ConflictGraph(
  private val conflictMap0: MutableMap<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
) {
  private val isolatedNodes0: MutableSet<BuildTargetIdentifier> = conflictMap0.filter { it.value.toSet().isEmpty() }.keys.toMutableSet()
  private val connectedNodes0: MutableSet<BuildTargetIdentifier> = (conflictMap0.keys - isolatedNodes0).toMutableSet()

  /**
   * Get all nodes that does not conflict with any other.
   *
   * An equivalent of set of all keys in conflict map, whose values are empty sets. Kept as variable for performance
   * reasons.
   */
  public val isolatedNodes: Set<BuildTargetIdentifier> = isolatedNodes0


  /**
   * Check if any nodes are still left in the graph
   */
  public fun isNotEmpty(): Boolean = conflictMap0.isNotEmpty()

  /**
   * Get all nodes that still contain conflicts
   *
   * An equivalent of set of all keys in conflict map, whose values are nonempty sets.
   */
  public val connectedNodes: Set<BuildTargetIdentifier> = connectedNodes0

  /**
   * Value field in this map is a set of nodes that are conflicting with the key one.
   */
  public val conflictMap: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>> = conflictMap0

  /**
   * Remove all nodes and make sure the cache variables are up to date
   */
  public fun removeAll(nodes: Set<BuildTargetIdentifier>) {
    val conflicts = nodes.flatMap { conflictMap0[it].orEmpty() }
    conflicts.forEach {
      conflictMap0.replace(it, conflictMap0[it].orEmpty() - nodes)
      if (conflictMap0[it].isNullOrEmpty()) {
        isolatedNodes0.add(it)
        connectedNodes0.remove(it)
      }
    }
    nodes.forEach { conflictMap0.remove(it) }
    isolatedNodes0.removeAll(nodes)
    connectedNodes0.removeAll(nodes)
  }
}