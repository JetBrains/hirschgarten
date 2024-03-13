package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ScalacOptionsItem
import ch.epfl.scala.bsp4j.SourcesItem
import org.jetbrains.bsp.LibraryItem
import org.jetbrains.bsp.utils.extractGoBuildTarget
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails

internal class ProjectDetailsToModuleDetailsTransformer(
  private val projectDetails: ProjectDetails,
) {
  private val targetsIndex = projectDetails.targets.associateBy { it.id }
  private val librariesIndex = projectDetails.libraries?.associateBy { it.id }

  fun moduleDetailsForTargetId(targetId: BuildTargetIdentifier): ModuleDetails {
    val target = calculateTarget(projectDetails, targetId)
    val allDependencies = allDependencies(target, librariesIndex)
    val sources = calculateSources(projectDetails, targetId)
    val resources = calculateResources(projectDetails, targetId)
    return ModuleDetails(
      target = target,
      sources = sources,
      resources = resources,
      dependenciesSources = calculateDependenciesSources(projectDetails, targetId),
      javacOptions = calculateJavacOptions(projectDetails, targetId),
      scalacOptions = calculateScalacOptions(projectDetails, targetId),
      pythonOptions = calculatePythonOptions(projectDetails, targetId),
      outputPathUris = emptyList(),
      libraryDependencies = librariesIndex?.keys?.intersect(allDependencies)?.map { it.uri },
      moduleDependencies = targetsIndex.keys.intersect(allDependencies).map { it.uri },
      defaultJdkName = projectDetails.defaultJdkName,
    )
  }

  private fun allDependencies(
    target: BuildTarget,
    librariesIndex: Map<BuildTargetIdentifier, LibraryItem>?,
  ): Set<BuildTargetIdentifier> {
    var librariesToVisit = target.dependencies
    var visited = emptySet<BuildTargetIdentifier>()
    while (librariesToVisit.isNotEmpty()) {
      val currentLib = librariesToVisit.first()
      librariesToVisit = librariesToVisit - currentLib
      visited = visited + currentLib
      librariesToVisit = librariesToVisit + (findLibrary(currentLib, librariesIndex) - visited)
    }
    return visited
  }

  private fun findLibrary(
    currentLib: BuildTargetIdentifier,
    librariesIndex: Map<BuildTargetIdentifier, LibraryItem>?,
  ) = librariesIndex?.get(currentLib)?.dependencies.orEmpty()

  private fun calculateTarget(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): BuildTarget =
    projectDetails.targets.first { it.id == targetId }

  private fun calculateSources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<SourcesItem> =
    projectDetails.sources.filter { it.target == targetId }

  private fun calculateResources(projectDetails: ProjectDetails, targetId: BuildTargetIdentifier): List<ResourcesItem> =
    projectDetails.resources.filter { it.target == targetId }

  private fun calculateDependenciesSources(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier,
  ): List<DependencySourcesItem> =
    projectDetails.dependenciesSources.filter { it.target == targetId }

  private fun calculateJavacOptions(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier,
  ): JavacOptionsItem? =
    projectDetails.javacOptions.firstOrNull { it.target == targetId }

  private fun calculateScalacOptions(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier,
  ): ScalacOptionsItem? =
    projectDetails.scalacOptions.firstOrNull { it.target == targetId }

  private fun calculatePythonOptions(
    projectDetails: ProjectDetails,
    targetId: BuildTargetIdentifier,
  ): PythonOptionsItem? =
    projectDetails.pythonOptions.firstOrNull { it.target == targetId }
}
