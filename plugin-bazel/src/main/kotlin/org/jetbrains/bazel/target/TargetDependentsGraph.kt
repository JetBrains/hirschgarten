package org.jetbrains.bazel.target

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem

internal class TargetDependentsGraph(targets: List<BuildTarget>, libraryItems: List<LibraryItem>?) {
  private val targetIdToDirectDependentIds = hashMapOf<Label, MutableSet<Label>>()

  init {
    for (targetInfo in targets) {
      val dependencies = targetInfo.dependencies
      for (dependency in dependencies) {
        targetIdToDirectDependentIds
          .computeIfAbsent(dependency) { hashSetOf<Label>() }
          .add(targetInfo.id)
      }
    }
    if (!libraryItems.isNullOrEmpty()) {
      for (libraryItem in libraryItems) {
        val dependencies = libraryItem.dependencies
        for (dependency in dependencies) {
          val dependentIds = targetIdToDirectDependentIds.computeIfAbsent(dependency) { hashSetOf() }
          dependentIds.add(libraryItem.id)
        }
      }
    }
  }

  fun directDependentIds(targetId: Label): Set<Label> = targetIdToDirectDependentIds.getOrDefault(targetId, emptySet())
}
