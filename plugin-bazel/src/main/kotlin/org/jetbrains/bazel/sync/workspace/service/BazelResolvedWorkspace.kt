package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget

data class BazelResolvedWorkspace(
  val targets: BuildTargetCollection,
  val libraries: List<LibraryItem> = listOf(),
  val hasError: Boolean = false,
)

data class BuildTargetCollection(
  internal val buildTargets: MutableList<RawBuildTarget> = mutableListOf(),
  internal val nonModuleTargets: MutableList<RawBuildTarget> = mutableListOf(),
) {
  fun addBuildTargets(targets: Collection<RawBuildTarget>) {
    buildTargets.addAll(targets)
  }

  fun addNonModuleTargets(targets: Collection<RawBuildTarget>) {
    nonModuleTargets.addAll(targets)
  }

  fun getTargets(): Sequence<RawBuildTarget> =
    sequence {
      // we want to overwrite non-module targets with build targets in case of a latter associateBy call
      yieldAll(nonModuleTargets)
      yieldAll(buildTargets)
    }

  fun getTargetIDs(): Sequence<Label> = getTargets().map { it.id }

  companion object {
    fun ofBuildTargets(targets: Collection<RawBuildTarget>) =
      BuildTargetCollection()
        .also { it.addBuildTargets(targets) }
  }
}
