package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import kotlin.reflect.KProperty

internal class TargetIdToModuleDetailsDelegate(
  private val projectDetails: ProjectDetails,
) {

  operator fun getValue(
    thisRef: Any?,
    property: KProperty<*>,
  ): Map<BuildTargetIdentifier, ModuleDetails> =
    projectDetails.targetsId.associateWith(this::toModuleDetails)

  private fun toModuleDetails(targetId: BuildTargetIdentifier): ModuleDetails =
    ModuleDetails(
      target = calculateTarget(targetId),
      allTargetsIds = projectDetails.targetsId,
      sources = calculateSources(targetId),
      resources = calculateResources(targetId),
      dependenciesSources = calculateDependenciesSources(targetId),
    )

  private fun calculateTarget(targetId: BuildTargetIdentifier): BuildTarget =
    projectDetails.targets.first { it.id == targetId }

  private fun calculateSources(targetId: BuildTargetIdentifier): List<SourcesItem> =
    projectDetails.sources.filter { it.target == targetId }

  private fun calculateResources(targetId: BuildTargetIdentifier): List<ResourcesItem> =
    projectDetails.resources.filter { it.target == targetId }

  private fun calculateDependenciesSources(targetId: BuildTargetIdentifier): List<DependencySourcesItem> =
    projectDetails.dependenciesSources.filter { it.target == targetId }
}
