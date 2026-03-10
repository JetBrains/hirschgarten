package org.jetbrains.bazel.sync.workspace.mapper

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

internal object BazelResolvedWorkspaceBuilder {

  fun build(targets: List<RawBuildTarget>, libraries: List<LibraryItem>, hasError: Boolean): BazelResolvedWorkspace {
    return BazelResolvedWorkspace(
      targets = targets,
      libraries = libraries,
      fileToTarget = calculateFileToTarget(targets),
      hasError = hasError,
    )
  }

  private fun calculateFileToTarget(targets: List<BuildTarget>): Map<Path, List<Label>> {
    val resultMap = HashMap<Path, MutableList<Label>>()
    for (target in targets) {
      target as RawBuildTarget
      for (source in target.sources) {
        val path = source.path
        val label = target.id
        resultMap.computeIfAbsent(path) { ArrayList() }.add(label)
      }
    }
    return resultMap
  }
}
