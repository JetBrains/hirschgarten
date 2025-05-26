package org.jetbrains.bazel.target

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem

internal class TargetDependentsGraph(targetIdToTargetInfo: Map<Label, BuildTarget>, libraryItems: List<LibraryItem>?) {
  private val targetIdToDirectDependentIds = hashMapOf<Label, MutableSet<Label>>()

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

  fun directDependentIds(targetId: Label): Set<Label> = targetIdToDirectDependentIds.getOrDefault(targetId, emptySet())
}
