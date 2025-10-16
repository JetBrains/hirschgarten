package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

class AllowManualTargetsSyncSection : BooleanScalarSection() {
  override val name = NAME
  override val default = false
  override val sectionKey = KEY
  override val doc =
    "If this option is set to true, build targets labeled with the manual tag can be synced " +
      "which will enable running and debugging them. Enabling this option will implicitly enable " +
      "the shard_sync option to allow labels expansion for Bazel query especially in cases " +
      "where derive_targets_from_directories is disabled."

  companion object {
    const val NAME = "allow_manual_targets_sync"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
