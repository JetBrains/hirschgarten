package org.jetbrains.bazel.target

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bsp.protocol.LibraryItem

internal class TargetDependentsGraph(targetIdToTargetInfo: Map<Label, BuildTargetInfo>, libraryItems: List<LibraryItem>?) {
  private val targetIdToDirectDependentIds = hashMapOf<Label, MutableSet<Label>>()

  init {
    targetIdToTargetInfo.entries.forEach { (targetId, targetInfo) ->
      val dependencies = targetInfo.dependencies
      dependencies.forEach { dependency ->
        val dependentIds = targetIdToDirectDependentIds.getOrPut(dependency.label()) { hashSetOf() }
        dependentIds.add(targetId)
      }
    }
    libraryItems?.forEach { libraryItem ->
      val dependencies = libraryItem.dependencies
      dependencies.forEach { dependency ->
        val dependentIds = targetIdToDirectDependentIds.getOrPut(dependency.label()) { hashSetOf() }
        dependentIds.add(libraryItem.id.label())
      }
    }
  }

  fun directDependentIds(targetId: Label): Set<Label> = targetIdToDirectDependentIds.getOrDefault(targetId, hashSetOf())
}
