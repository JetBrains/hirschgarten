package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SYNC_FLAGS_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

internal class SyncFlagsSection : FlagListSection(COMMAND) {
  override val sectionKey = SYNC_FLAGS_KEY
  override val doc =
    "A set of flags that get passed to build during all sync actions. Unlike" +
      "'build_flags', these are not used for run configurations, so use 'sync_flags' " +
      "only when necessary, as they can defeat caching."

  companion object {
    const val COMMAND = "sync"
  }
}
