package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import kotlin.reflect.KProperty

internal class NonOverlappingTargetsDelegate(
  private val overlappingTargetsGraph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
) {

  operator fun getValue(
    thisRef: Any?,
    property: KProperty<*>,
  ): Set<BuildTargetIdentifier> {
    val allTargets = overlappingTargetsGraph.keys

    LOGGER.trace { "Calculating non overlapping targets for $allTargets..." }

    return allTargets
      .fold(emptySet(), this::addTargetToSetIfNoneOfItsNeighborsIsAdded)
      .also {
        LOGGER.trace {
          "Calculating non overlapping targets for $allTargets done! Calculated non overlapping targets: $it."
        }
      }
  }

  private fun addTargetToSetIfNoneOfItsNeighborsIsAdded(
    nonOverlappingTargetsAcc: Set<BuildTargetIdentifier>,
    target: BuildTargetIdentifier,
  ): Set<BuildTargetIdentifier> {
    val shouldNotTargetBeAddedToSet = isAnyOfNeighborsAddedToSet(nonOverlappingTargetsAcc, target)

    return if (shouldNotTargetBeAddedToSet) nonOverlappingTargetsAcc
    else (nonOverlappingTargetsAcc + target).also { LOGGER.trace { "Adding $target to non overlapping targets." } }
  }

  private fun isAnyOfNeighborsAddedToSet(
    nonOverlappingTargetsAcc: Set<BuildTargetIdentifier>,
    target: BuildTargetIdentifier,
  ): Boolean {
    val neighbors = overlappingTargetsGraph[target] ?: emptySet()

    LOGGER.trace {
      "Checking that any of $target overlapping targets $neighbors " +
        "is already included in the non overlapping targets set..."
    }

    return isAnyTargetAddedToSet(nonOverlappingTargetsAcc, neighbors)
      .also { LOGGER.trace { "Checking done! Result: $it." } }
  }

  private fun isAnyTargetAddedToSet(
    nonOverlappingTargetsAcc: Set<BuildTargetIdentifier>,
    targets: Collection<BuildTargetIdentifier>,
  ): Boolean = targets.any { it in nonOverlappingTargetsAcc }

  companion object {
    private val LOGGER = logger<NonOverlappingTargetsDelegate>()
  }
}
