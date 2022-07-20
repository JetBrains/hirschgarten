package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace

internal object NonOverlappingTargets {

  private val log = logger<NonOverlappingTargets>()

  operator fun invoke(
    allTargets: Set<BuildTarget>,
    overlappingTargetsGraph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
  ): Set<BuildTargetIdentifier> {
    log.trace { "Calculating non overlapping targets for $allTargets..." }

    return allTargets
      .fold<BuildTarget, Set<BuildTargetIdentifier>>(emptySet()) { acc, targetToAdd ->
        addTargetToSetIfNoneOfItsNeighborsIsAddedAndDoTheSameForDependencies(
          acc,
          targetToAdd,
          allTargets,
          overlappingTargetsGraph
        )
      }
      .also {
        log.trace {
          "Calculating non overlapping targets for $allTargets done! Calculated non overlapping targets: $it."
        }
      }
  }

  // TODO this should be reimplemented - now it can throw stack overflow exception
  private fun addTargetToSetIfNoneOfItsNeighborsIsAddedAndDoTheSameForDependencies(
    nonOverlappingTargetsAcc: Set<BuildTargetIdentifier>,
    target: BuildTarget,
    allTargets: Set<BuildTarget>,
    overlappingTargetsGraph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
  ): Set<BuildTargetIdentifier> {
    val nonOverlappingTargetsAccWithTarget =
      addTargetToSetIfNoneOfItsNeighborsIsAdded(nonOverlappingTargetsAcc, target, overlappingTargetsGraph)

    return target.dependencies
      .mapNotNull { mapTargetIdToTarget(allTargets, it) }
      .fold(nonOverlappingTargetsAccWithTarget) { acc, targetToAdd ->
        addTargetToSetIfNoneOfItsNeighborsIsAddedAndDoTheSameForDependencies(
          acc,
          targetToAdd,
          allTargets,
          overlappingTargetsGraph
        )
      }
  }

  private fun addTargetToSetIfNoneOfItsNeighborsIsAdded(
    nonOverlappingTargetsAcc: Set<BuildTargetIdentifier>,
    target: BuildTarget,
    overlappingTargetsGraph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
  ): Set<BuildTargetIdentifier> {
    val shouldNotTargetBeAddedToSet =
      isAnyOfNeighborsAddedToSet(nonOverlappingTargetsAcc, target.id, overlappingTargetsGraph)

    return if (shouldNotTargetBeAddedToSet) nonOverlappingTargetsAcc
    else (nonOverlappingTargetsAcc + target.id).also { log.trace { "Adding $target to non overlapping targets." } }
  }

  private fun isAnyOfNeighborsAddedToSet(
    nonOverlappingTargetsAcc: Set<BuildTargetIdentifier>,
    target: BuildTargetIdentifier,
    overlappingTargetsGraph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
  ): Boolean {
    val neighbors = overlappingTargetsGraph[target] ?: emptySet()

    log.trace {
      "Checking that any of $target overlapping targets $neighbors " +
        "is already included in the non overlapping targets set..."
    }

    return isAnyTargetAddedToSet(nonOverlappingTargetsAcc, neighbors)
      .also { log.trace { "Checking done! Result: $it." } }
  }

  private fun isAnyTargetAddedToSet(
    nonOverlappingTargetsAcc: Set<BuildTargetIdentifier>,
    targets: Collection<BuildTargetIdentifier>,
  ): Boolean =
    targets.any { it in nonOverlappingTargetsAcc }

  private fun mapTargetIdToTarget(allTargets: Set<BuildTarget>, targetId: BuildTargetIdentifier): BuildTarget? =
    allTargets.find { it.id == targetId }
}
