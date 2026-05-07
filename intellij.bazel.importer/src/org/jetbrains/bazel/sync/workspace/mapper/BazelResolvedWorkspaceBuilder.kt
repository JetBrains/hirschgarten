package org.jetbrains.bazel.sync.workspace.mapper

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

@ApiStatus.Internal
object BazelResolvedWorkspaceBuilder {

  fun build(rootTargets: Set<Label>, targets: List<RawBuildTarget>, hasError: Boolean = false): BazelResolvedWorkspace {
    return BazelResolvedWorkspace(
      rootTargets = rootTargets,
      targets = targets,
      fileToTarget = calculateFileToTarget(targets),
      hasError = hasError,
    )
  }

  private fun calculateFileToTarget(targets: List<BuildTarget>): Map<Path, List<RawBuildTarget>> {
    val resultMap = HashMap<Path, MutableList<RawBuildTarget>>()
    for (target in targets) {
      target as RawBuildTarget
      for (source in target.sources) {
        val path = source.path
        resultMap.computeIfAbsent(path) { ArrayList() }.add(target)
      }
    }
    return resultMap
  }
}
