package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.magicmetamodel.extensions.reduceSets

public object OverlappingTargetsGraph {

  private val log = logger<OverlappingTargetsGraph>()

  public operator fun invoke(
    targetsDetailsForDocumentProvider: TargetsDetailsForDocumentProvider,
  ): Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>> {
    log.trace { "Calculating overlapping targets graph..." }

    return targetsDetailsForDocumentProvider.getAllDocuments().asSequence()
      .map { targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(it) }
      .flatMap { generateEdgesForOverlappingTargetsForAllTargets(it) }
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.reduceSets() }
      .mapValues { filterGivenTargetFromOverlappingTargetsAndMapToSet(it.key, it.value) }
      .also { log.trace { "Calculating overlapping targets graph done! Graph: $it." } }
  }

  private fun generateEdgesForOverlappingTargetsForAllTargets(
    overlappingTargets: Set<BuildTargetIdentifier>,
  ): List<Pair<BuildTargetIdentifier, Set<BuildTargetIdentifier>>> {
    ProgressManager.checkCanceled()

    return overlappingTargets.map { Pair(it, overlappingTargets) }
  }

  private fun filterGivenTargetFromOverlappingTargetsAndMapToSet(
    target: BuildTargetIdentifier,
    overlappingTargets: Set<BuildTargetIdentifier>,
  ): Set<BuildTargetIdentifier> =
    overlappingTargets.minus(target)
}
