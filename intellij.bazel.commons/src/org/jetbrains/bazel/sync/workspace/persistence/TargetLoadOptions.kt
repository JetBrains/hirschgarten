package org.jetbrains.bazel.sync.workspace.persistence

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTargetData

/**
 * Independently loadable section of a target
 */
@ApiStatus.Internal
enum class TargetSection {
  // base directory, generator name, `isManual`, `isWorkspace`, `isTestOnly`, tags
  INFO,

  // target deps
  DEPS,

  // file collections like: sources, generated sources, etc.
  FILE_SETS,
}

@ApiStatus.Internal
sealed interface BuildTargetLoadHint {
  data object All : BuildTargetLoadHint
  data object None : BuildTargetLoadHint
  data class Subset(val data: Set<Class<out BuildTargetData>>) : BuildTargetLoadHint
}

/**
 * Describes which sections of a target are loaded eagerly.
 * Sections not loaded eagerly will be loaded at access time.
 *
 * @property sections Eagerly loaded sections, everything not listed here is loaded lazily
 * @property targetData Select what [BuildTargetData] types to load eagerly
 */
@ApiStatus.Internal
data class TargetLoadOptions(
  val sections: Set<TargetSection>,
  val targetData: BuildTargetLoadHint,
) {
  companion object {
    // entire target
    val ALL: TargetLoadOptions = TargetLoadOptions(TargetSection.entries.toSet(), BuildTargetLoadHint.All)

    // only target summary
    val SUMMARY: TargetLoadOptions = TargetLoadOptions(setOf(TargetSection.INFO), BuildTargetLoadHint.None)

    // only target key
    val MINIMAL: TargetLoadOptions = TargetLoadOptions(setOf(), BuildTargetLoadHint.None)
  }
}

@ApiStatus.Internal
fun TargetLoadOptions(
  vararg sections: TargetSection,
  targetData: BuildTargetLoadHint = BuildTargetLoadHint.None,
): TargetLoadOptions = TargetLoadOptions(sections.toSet(), targetData)
