package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.LibraryItem

internal data class LibraryGraphDependencies(
  val libraryDependencies: Set<BuildTargetIdentifier>,
  val targetDependencies: Set<BuildTargetIdentifier>,
)

internal class LibraryGraph(libraries: List<LibraryItem>) {
  private val graph = libraries.associate { it.id to it.dependencies }

  fun findAllTransitiveDependencies(target: BuildTarget): LibraryGraphDependencies {
    val toVisit = target.dependencies.toMutableSet()
    val visited = mutableSetOf<BuildTargetIdentifier>(target.id)

    val resultLibraries = mutableSetOf<BuildTargetIdentifier>()
    val resultTargets = mutableSetOf<BuildTargetIdentifier>()

    while (toVisit.isNotEmpty()) {
      val currentNode = toVisit.first()
      toVisit -= currentNode

      if (currentNode !in visited) {
        toVisit += graph[currentNode].orEmpty()
        visited += currentNode

        currentNode.addToCorrectResultSet(resultLibraries, resultTargets)
      }
    }

    return LibraryGraphDependencies(
      libraryDependencies = resultLibraries,
      targetDependencies = resultTargets,
    )
  }

  private fun BuildTargetIdentifier.addToCorrectResultSet(
    resultLibraries: MutableSet<BuildTargetIdentifier>,
    resultTargets: MutableSet<BuildTargetIdentifier>,
  ) {
    val isCurrentNodeLibrary = this in graph
    if (isCurrentNodeLibrary) {
      resultLibraries += this
    } else {
      resultTargets += this
    }
  }
}
