package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.ALLOW_MANUAL_TARGETS_SYNC_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

internal class AllowManualTargetsSyncSection : BooleanScalarSection() {
  override val sectionKey = ALLOW_MANUAL_TARGETS_SYNC_KEY
  override val doc =
    "If this option is set to true, build targets labeled with the manual tag can be synced " +
      "which will enable running and debugging them. Enabling this option will implicitly enable " +
      "the shard_sync option to allow labels expansion for Bazel query especially in cases " +
      "where derive_targets_from_directories is disabled."
}
