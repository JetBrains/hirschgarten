package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails

internal object TargetIdToModuleDetails {

  operator fun invoke(projectDetails: ProjectDetails): Map<BuildTargetIdentifier, ModuleDetails> =
    projectDetails.targetsId.associateWith { toModuleDetails(projectDetails, it) }

  private fun toModuleDetails(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): ModuleDetails =
    ModuleDetails(
      target = calculateTarget(projectDetails, targetId),
      allTargetsIds = projectDetails.targetsId,
      sources = calculateSources(projectDetails, targetId),
      resources = calculateResources(projectDetails, targetId),
      dependenciesSources = calculateDependenciesSources(projectDetails, targetId),
    )

  private fun calculateTarget(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): BuildTarget =
    projectDetails.targets.first { it.id == targetId }

  private fun calculateSources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<SourcesItem> =
    projectDetails.sources.filter { it.target == targetId }

  private fun calculateResources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<ResourcesItem> =
    projectDetails.resources.filter { it.target == targetId }

  private fun calculateDependenciesSources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<DependencySourcesItem> =
    projectDetails.dependenciesSources.filter { it.target == targetId }
}
