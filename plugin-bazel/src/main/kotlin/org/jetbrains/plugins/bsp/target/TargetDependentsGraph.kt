package org.jetbrains.plugins.bsp.target

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

internal class TargetDependentsGraph(targetIdToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfo>, libraryItems: List<LibraryItem>?) {
  private val targetIdToDirectDependentIds = hashMapOf<BuildTargetIdentifier, MutableSet<BuildTargetIdentifier>>()

  init {
    targetIdToTargetInfo.entries.forEach { (targetId, targetInfo) ->
      val dependencies = targetInfo.dependencies
      dependencies.forEach { dependency ->
        val dependentIds = targetIdToDirectDependentIds.getOrPut(dependency) { hashSetOf() }
        dependentIds.add(targetId)
      }
    }
    libraryItems?.forEach { libraryItem ->
      val dependencies = libraryItem.dependencies
      dependencies.forEach { dependency ->
        val dependentIds = targetIdToDirectDependentIds.getOrPut(dependency) { hashSetOf() }
        dependentIds.add(libraryItem.id)
      }
    }
  }

  fun directDependentIds(targetId: BuildTargetIdentifier): Set<BuildTargetIdentifier> =
    targetIdToDirectDependentIds.getOrDefault(targetId, hashSetOf())
}
