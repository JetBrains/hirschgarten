package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.IntScalarSection

class TargetShardSizeSection : IntScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Number of targets per build shard."

  companion object {
    const val NAME = "target_shard_size"
    val KEY = SectionKey<Int>(NAME)
  }
}
