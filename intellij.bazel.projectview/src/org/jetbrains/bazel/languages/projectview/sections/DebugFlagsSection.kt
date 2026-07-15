package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.DEBUG_FLAGS_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

internal class DebugFlagsSection : FlagListSection(COMMAND_RUN, COMMAND_TEST) {
  override val sectionKey = DEBUG_FLAGS_KEY
  override val doc = "A set of flags that get passed as arguments to all run nad test commands invocations in debug mode."

  companion object {
    const val COMMAND_RUN = "run"
    const val COMMAND_TEST = "test"
  }
}
