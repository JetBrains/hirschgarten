package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.TARGET_SHARD_SIZE_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.IntScalarSection

internal class TargetShardSizeSection : IntScalarSection() {
  override val sectionKey = TARGET_SHARD_SIZE_KEY
  override val doc = "Number of targets per build shard."
}
