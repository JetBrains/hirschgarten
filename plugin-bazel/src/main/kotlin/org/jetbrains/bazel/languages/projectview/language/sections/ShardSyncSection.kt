package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.ScalarSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class ShardSyncSection : ScalarSection<Boolean>() {
  override val name: String = NAME

  override fun getSectionKey(): SectionKey<Boolean> = sectionKey

  override fun fromRawValues(rawValues: List<String>): Boolean? {
    if (rawValues.size != 1) {
      return null
    }
    return rawValues[0].toBooleanStrictOrNull()
  }

  companion object {
    const val NAME = "shard_sync"
    val sectionKey = SectionKey<Boolean>(NAME)
  }
}
