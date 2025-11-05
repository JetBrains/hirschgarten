package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

class DebugFlagsSection : FlagListSection(COMMAND) {
  override val name = NAME
  override val default = emptyList<String>()
  override val sectionKey = KEY
  override val doc = "A set of flags that get passed to all debug command invocations as arguments."

  companion object {
    const val NAME = "debug_flags"
    val KEY = SectionKey<List<String>>(NAME)
    const val COMMAND = "debug"
  }
}
