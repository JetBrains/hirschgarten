package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.alsoIfNull
import org.jetbrains.magicmetamodel.LibraryItem
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal object TargetIdToModuleDetailsMap {

  val log = Logger.getInstance(TargetIdToModuleDetailsMap::class.java)

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
        libraryDependencies = projectDetails.libraries?.let { libraryDependencies(target, it) } ,
        moduleDependencies = target.dependencies.filter { dependency ->
          projectDetails.targets.any { it.id.uri == dependency.uri }
        },
        allTargetsIds = projectDetails.targetsId,
        sources = calculateSources(projectDetails, targetId),
        resources = calculateResources(projectDetails, targetId),
        dependenciesSources = calculateDependenciesSources(projectDetails, targetId),
        javacOptions = calculateJavacOptions(projectDetails, targetId),
        outputPathUris = emptyList(),
      )
    }
  }

  private fun libraryDependencies(target: BuildTarget, libraries: List<LibraryItem>): List<BuildTargetIdentifier> {
    var librariesToVisit =  target.dependencies.filter { dependency -> libraries.any { it.id.uri == dependency.uri } }
    var visited = emptySet<BuildTargetIdentifier>();
    while(librariesToVisit.isNotEmpty()) {
      val currentLib = librariesToVisit.first()
      librariesToVisit = librariesToVisit - currentLib
      visited = visited + currentLib
      librariesToVisit = librariesToVisit + (findLibraryOrLogError(libraries, currentLib).orEmpty() - visited)
    }
    return visited.toList()
  }

  private fun findLibraryOrLogError(
    libraries: List<LibraryItem>,
    currentLib: BuildTargetIdentifier
  ) = libraries.find { it.id == currentLib }?.dependencies
      .alsoIfNull { log.error("Could not find library: ${currentLib.uri}") }

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
      libraryDependencies = emptyList(),
      moduleDependencies = emptyList(),
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
