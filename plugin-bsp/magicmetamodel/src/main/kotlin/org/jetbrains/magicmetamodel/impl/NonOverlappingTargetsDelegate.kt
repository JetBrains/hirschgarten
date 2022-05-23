package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import kotlin.reflect.KProperty

internal class NonOverlappingTargetsDelegate(
  private val allTargets: Set<BuildTarget>,
  private val overlappingTargetsGraph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
) {

  operator fun getValue(
    thisRef: Any?,
    property: KProperty<*>,
  ): Set<BuildTargetIdentifier> {
    log.trace { "Calculating non overlapping targets for $allTargets..." }

    return allTargets
      .fold(emptySet(), this::addTargetToSetIfNoneOfItsNeighborsIsAddedAndDoTheSameForDependencies)
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
  ): Set<BuildTargetIdentifier> {
    val nonOverlappingTargetsAccWithTarget = addTargetToSetIfNoneOfItsNeighborsIsAdded(nonOverlappingTargetsAcc, target)

    return target.dependencies
      .mapNotNull { mapTargetIdToTarget(it) }
      .fold(nonOverlappingTargetsAccWithTarget, ::addTargetToSetIfNoneOfItsNeighborsIsAddedAndDoTheSameForDependencies)
  }

  private fun addTargetToSetIfNoneOfItsNeighborsIsAdded(
    nonOverlappingTargetsAcc: Set<BuildTargetIdentifier>,
    target: BuildTarget,
  ): Set<BuildTargetIdentifier> {
    val shouldNotTargetBeAddedToSet = isAnyOfNeighborsAddedToSet(nonOverlappingTargetsAcc, target.id)

    return if (shouldNotTargetBeAddedToSet) nonOverlappingTargetsAcc
    else (nonOverlappingTargetsAcc + target.id).also { log.trace { "Adding $target to non overlapping targets." } }
  }

  private fun isAnyOfNeighborsAddedToSet(
    nonOverlappingTargetsAcc: Set<BuildTargetIdentifier>,
    target: BuildTargetIdentifier,
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
  ): Boolean = targets.any { it in nonOverlappingTargetsAcc }

  private fun mapTargetIdToTarget(targetId: BuildTargetIdentifier): BuildTarget? =
    allTargets.find { it.id == targetId }

  companion object {
    private val log = logger<NonOverlappingTargetsDelegate>()
  }
}
