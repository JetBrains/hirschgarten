package org.jetbrains.magicmetamodel.impl

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import org.jetbrains.magicmetamodel.MagicMetaModelTemporaryFacade
import org.jetbrains.magicmetamodel.extensions.reduceSets
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId

public object OverlappingTargetsGraph {
  private val log = logger<OverlappingTargetsGraph>()

  // only for tests, in the following PRs will be removed
  public operator fun invoke(
    targetsDetailsForDocumentProvider: TargetsDetailsForDocumentProvider,
  ): Map<BuildTargetId, Set<BuildTargetId>> =
    invoke(MagicMetaModelTemporaryFacade(targetsDetailsForDocumentProvider))

  public operator fun invoke(
    facade: MagicMetaModelTemporaryFacade,
  ): Map<BuildTargetId, Set<BuildTargetId>> {
    log.trace { "Calculating overlapping targets graph..." }

    return facade.allDocuments().asSequence()
      .map { facade.getTargetsForFile(it) }
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
