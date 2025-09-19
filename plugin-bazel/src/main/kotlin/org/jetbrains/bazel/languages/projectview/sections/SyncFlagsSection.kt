package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

class SyncFlagsSection : FlagListSection(COMMAND) {
  override val name = NAME
  override val default = emptyList<Flag>()
  override val sectionKey = KEY
  override val doc =
    "A set of flags that get passed to build during all sync actions. Unlike" +
      "'build_flags', these are not used for run configurations, so use 'sync_flags' " +
      "only when necessary, as they can defeat caching."

  companion object {
    const val NAME = "sync_flags"
    val KEY = SectionKey<List<Flag>>(NAME)
    const val COMMAND = "sync"
  }
}
