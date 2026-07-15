package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.BUILD_FLAGS_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

internal class BuildFlagsSection : FlagListSection(COMMAND) {
  override val sectionKey = BUILD_FLAGS_KEY
  override val doc =
    "A set of flags that get passed to all build command invocations as arguments. This" +
      "includes both sync and run configuration actions."

  companion object {
    const val COMMAND = "build"
  }
}
