package org.jetbrains.magicmetamodel.impl

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import org.jetbrains.magicmetamodel.extensions.reduceSets
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId

public object OverlappingTargetsGraph {

  private val log = logger<OverlappingTargetsGraph>()

  public operator fun invoke(
    targetsDetailsForDocumentProvider: TargetsDetailsForDocumentProvider,
  ): Map<BuildTargetId, Set<BuildTargetId>> {
    log.trace { "Calculating overlapping targets graph..." }

    return targetsDetailsForDocumentProvider.getAllDocuments().asSequence()
      .map { targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(it) }
      .flatMap { generateEdgesForOverlappingTargetsForAllTargets(it) }
      .groupBy({ it.first }, { it.second })
      .mapValues { it.value.reduceSets().toSet() }
      .mapValues { filterGivenTargetFromOverlappingTargetsAndMapToSet(it.key, it.value) }
      .also { log.trace { "Calculating overlapping targets graph done! Graph: $it." } }
  }

  private fun generateEdgesForOverlappingTargetsForAllTargets(
    overlappingTargets: Set<BuildTargetId>,
  ): List<Pair<BuildTargetId, Set<BuildTargetId>>> =
    overlappingTargets.map { Pair(it, overlappingTargets) }

  private fun filterGivenTargetFromOverlappingTargetsAndMapToSet(
    target: BuildTargetId,
    overlappingTargets: Set<BuildTargetId>,
  ): Set<BuildTargetId> =
    overlappingTargets.minus(target)
}
