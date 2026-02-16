package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

class DebugFlagsSection : FlagListSection(COMMAND_RUN, COMMAND_TEST) {
  override val name = NAME
  override val default = emptyList<String>()
  override val sectionKey = KEY
  override val doc = "A set of flags that get passed as arguments to all run nad test commands invocations in debug mode."

  companion object {
    const val NAME = "debug_flags"
    val KEY = SectionKey<List<String>>(NAME)
    const val COMMAND_RUN = "run"
    const val COMMAND_TEST = "test"
  }
}
