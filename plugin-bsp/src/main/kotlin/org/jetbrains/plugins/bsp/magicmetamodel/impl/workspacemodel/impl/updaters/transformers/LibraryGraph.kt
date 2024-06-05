package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.plugins.bsp.config.BspFeatureFlags

internal data class LibraryGraphDependencies(
  val libraryDependencies: Set<BuildTargetIdentifier>,
  val moduleDependencies: Set<BuildTargetIdentifier>,
)

internal class LibraryGraph(libraries: List<LibraryItem>) {
  private val graph = libraries.associate { it.id to it.dependencies }

  fun calculateAllDependencies(
    target: BuildTarget,
    includesTransitive: Boolean = !BspFeatureFlags.isWrapLibrariesInsideModulesEnabled,
  ): LibraryGraphDependencies =
    if (includesTransitive)
      calculateAllTransitiveDependencies(target)
    else
      calculateDirectDependencies(target)

  private fun calculateAllTransitiveDependencies(target: BuildTarget): LibraryGraphDependencies {
    val toVisit = target.dependencies.toMutableSet()
    val visited = mutableSetOf<BuildTargetIdentifier>(target.id)

    val resultLibraries = mutableSetOf<BuildTargetIdentifier>()
    val resultModules = mutableSetOf<BuildTargetIdentifier>()

    while (toVisit.isNotEmpty()) {
      val currentNode = toVisit.first()
      toVisit -= currentNode

      if (currentNode !in visited) {
        // don't traverse further when hitting modules
        if (currentNode.isCurrentNodeLibrary())
          toVisit += graph[currentNode].orEmpty()
        visited += currentNode

        currentNode.addToCorrectResultSet(resultLibraries, resultModules)
      }
    }

    return LibraryGraphDependencies(
      libraryDependencies = resultLibraries,
      moduleDependencies = resultModules,
    )
  }

  private fun calculateDirectDependencies(target: BuildTarget): LibraryGraphDependencies {
    val (libraryDependencies, moduleDependencies) =
      target.dependencies.partition { it.isCurrentNodeLibrary() }
    return LibraryGraphDependencies(
      libraryDependencies = libraryDependencies.toSet(),
      moduleDependencies = moduleDependencies.toSet(),
    )
  }

  private fun BuildTargetIdentifier.isCurrentNodeLibrary() = this in graph

  private fun BuildTargetIdentifier.addToCorrectResultSet(
    resultLibraries: MutableSet<BuildTargetIdentifier>,
    resultModules: MutableSet<BuildTargetIdentifier>,
  ) {
    if (isCurrentNodeLibrary()) {
      resultLibraries += this
    } else {
      resultModules += this
    }
  }
}
