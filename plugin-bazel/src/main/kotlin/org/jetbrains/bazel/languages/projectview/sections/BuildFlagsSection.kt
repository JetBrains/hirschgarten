package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

class BuildFlagsSection : FlagListSection(COMMAND) {
  override val name = NAME
  override val default = emptyList<String>()
  override val sectionKey = KEY
  override val doc =
    "A set of flags that get passed to all build command invocations as arguments. This" +
      "includes both sync and run configuration actions."

  companion object {
    const val NAME = "build_flags"
    val KEY = SectionKey<List<String>>(NAME)
    const val COMMAND = "build"
  }
}
