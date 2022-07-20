package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import org.jetbrains.magicmetamodel.extensions.reduceSets

internal object OverlappingTargetsGraph {

  private val log = logger<OverlappingTargetsGraph>()

  operator fun invoke(
    targetsDetailsForDocumentProvider: TargetsDetailsForDocumentProvider,
  ): Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>> {
    log.trace { "Calculating overlapping targets graph..." }

    return targetsDetailsForDocumentProvider.getAllDocuments()
      .map(targetsDetailsForDocumentProvider::getTargetsDetailsForDocument)
      .flatMap(this::generateEdgesForOverlappingTargetsForAllTargets)
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.reduceSets() }
      .also { log.trace { "Calculating overlapping targets graph done! Graph: $it." } }
  }

  private fun generateEdgesForOverlappingTargetsForAllTargets(
    overlappingTargets: List<BuildTargetIdentifier>,
  ): List<Pair<BuildTargetIdentifier, Set<BuildTargetIdentifier>>> =
    overlappingTargets.map { generateEdgesForOverlappingTargetsForOneTarget(it, overlappingTargets) }

  private fun generateEdgesForOverlappingTargetsForOneTarget(
    target: BuildTargetIdentifier,
    overlappingTargets: List<BuildTargetIdentifier>,
  ): Pair<BuildTargetIdentifier, Set<BuildTargetIdentifier>> {
    log.trace { "Calculating overlapping targets for $target..." }

    val targetEdges = filterGivenTargetFromOverlappingTargetsAndMapToSet(target, overlappingTargets)

    log.trace { "Calculating overlapping targets for $target done. Overlapping targets: $targetEdges." }

    return Pair(target, targetEdges)
  }

  private fun filterGivenTargetFromOverlappingTargetsAndMapToSet(
    target: BuildTargetIdentifier,
    overlappingTargets: List<BuildTargetIdentifier>,
  ): Set<BuildTargetIdentifier> =
    overlappingTargets
      .filter { it != target }
      .toSet()
}
