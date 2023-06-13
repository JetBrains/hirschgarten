package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal object TargetIdToModuleDetailsMap {

  operator fun invoke(
    projectDetails: ProjectDetails,
    projectBasePath: Path
  ): Map<BuildTargetIdentifier, ModuleDetails> =
    projectDetails.targetsId.associateWith { toModuleDetails(projectDetails, projectBasePath, it) }

  private fun toModuleDetails(
    projectDetails: ProjectDetails,
    projectBasePath: Path,
    targetId: BuildTargetIdentifier
  ): ModuleDetails {
    val target = calculateTarget(projectDetails, targetId)
    return if (target.isRoot(projectBasePath)) {
      toRootModuleDetails(projectDetails, target)
    } else {
      ModuleDetails(
        target = target,
        allTargetsIds = projectDetails.targetsId,
        sources = calculateSources(projectDetails, targetId),
        resources = calculateResources(projectDetails, targetId),
        dependenciesSources = calculateDependenciesSources(projectDetails, targetId),
        javacOptions = calculateJavacOptions(projectDetails, targetId),
        outputPathUris = emptyList(),
      )
    }
  }

  private fun toRootModuleDetails(
    projectDetails: ProjectDetails,
    target: BuildTarget,
  ): ModuleDetails =
    ModuleDetails(
      target = target,
      allTargetsIds = projectDetails.targetsId,
      sources = emptyList(),
      resources = emptyList(),
      dependenciesSources = calculateDependenciesSources(projectDetails, target.id),
      javacOptions = calculateJavacOptions(projectDetails, target.id),
      outputPathUris = calculateAllOutputPaths(projectDetails),
    )

  private fun calculateTarget(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): BuildTarget =
    projectDetails.targets.first { it.id == targetId }

  private fun calculateSources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<SourcesItem> =
    projectDetails.sources.filter { it.target == targetId }

  private fun calculateResources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<ResourcesItem> =
    projectDetails.resources.filter { it.target == targetId }

  private fun calculateDependenciesSources(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier
  ): List<DependencySourcesItem> =
    projectDetails.dependenciesSources.filter { it.target == targetId }

  private fun calculateJavacOptions(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier
  ): JavacOptionsItem? =
    projectDetails.javacOptions.firstOrNull { it.target == targetId }

  private fun calculateAllOutputPaths(projectDetails: ProjectDetails): List<String> =
    projectDetails.outputPathUris

  private fun BuildTarget.isRoot(projectBasePath: Path): Boolean =
    this.baseDirectory?.let { URI.create(it).toPath() } == projectBasePath
}
