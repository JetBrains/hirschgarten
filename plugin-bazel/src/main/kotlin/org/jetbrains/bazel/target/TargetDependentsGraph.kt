package org.jetbrains.bazel.target

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

internal class TargetDependentsGraph(targets: List<RawBuildTarget>, libraryItems: List<LibraryItem>?) {
  private val targetIdToDirectDependentIds = hashMapOf<CanonicalLabel, MutableSet<CanonicalLabel>>()

  init {
    for (targetInfo in targets) {
      val dependencies = targetInfo.dependencies
      for (dependency in dependencies) {
        targetIdToDirectDependentIds
          .computeIfAbsent(dependency) { hashSetOf<CanonicalLabel>() }
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

  fun directDependentIds(targetId: CanonicalLabel): Set<CanonicalLabel> = targetIdToDirectDependentIds.getOrDefault(targetId, emptySet())
}
