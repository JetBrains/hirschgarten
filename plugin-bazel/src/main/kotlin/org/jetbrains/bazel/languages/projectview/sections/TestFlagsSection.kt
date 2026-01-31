package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

class TestFlagsSection : FlagListSection(COMMAND) {
  override val name = NAME
  override val default = emptyList<String>()
  override val sectionKey = KEY
  override val doc = "A set of flags that get passed to all test command invocations as arguments."

  companion object {
    const val NAME = "test_flags"
    val KEY = SectionKey<List<String>>(NAME)
    const val COMMAND = "test"
  }
}
