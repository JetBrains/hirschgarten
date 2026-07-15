package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.TEST_FLAGS_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

internal class TestFlagsSection : FlagListSection(COMMAND) {
  override val sectionKey = TEST_FLAGS_KEY
  override val doc = "A set of flags that get passed to all test command invocations as arguments."

  companion object {
    const val COMMAND = "test"
  }
}
