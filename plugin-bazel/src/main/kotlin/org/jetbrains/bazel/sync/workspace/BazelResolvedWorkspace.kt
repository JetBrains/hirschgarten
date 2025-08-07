package org.jetbrains.bazel.sync.workspace

import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.util.EnumMap

data class BazelResolvedWorkspace(
  val targets: BuildTargetCollection,
  val libraries: List<LibraryItem> = listOf(),
  val hasError: Boolean = false,
)

enum class BuildTargetClassifier {
  BUILD_TARGET,
  NON_MODULE_TARGET
}

class BuildTargetCollection{
  internal val specifierToTargets: MutableMap<BuildTargetClassifier, MutableMap<Label, RawBuildTarget>> = EnumMap(BuildTargetClassifier::class.java)

  fun addTargets(classifier: BuildTargetClassifier, targets: Collection<RawBuildTarget>) {
    specifierToTargets.getOrPut(classifier) { mutableMapOf() }
      .putAll(targets.associateBy { it.id })
  }

  fun getTargets(classifier: BuildTargetClassifier): Collection<RawBuildTarget> = specifierToTargets[classifier]?.values ?: emptyList()

  fun getTargets(): Sequence<RawBuildTarget> = specifierToTargets.values
    .asSequence()
    .flatMap { it.values }

  fun getTargetIDs(): Sequence<Label> = specifierToTargets.values
    .asSequence()
    .flatMap { it.keys }

  companion object {
    fun ofBuildTargets(targets: Collection<RawBuildTarget>) = BuildTargetCollection()
      .also { it.addTargets(BuildTargetClassifier.BUILD_TARGET, targets) }
  }
}
