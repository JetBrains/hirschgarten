package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.PYTHON_DEBUG_FLAGS_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.FlagListSection

internal class PythonDebugFlagsSection : FlagListSection(COMMAND_RUN, COMMAND_TEST) {
  override val sectionKey = PYTHON_DEBUG_FLAGS_KEY
  override val doc =
    "A set of flags that get passed as arguments to run and test commands invocations in debug mode. " +
    "These flags are only used when the Python support is enabled in Bazel." +
    "They are added after flags specified in debug_flags section (in case of duplicates python_debug_flags have higher priority)."

  companion object {
    const val COMMAND_RUN = "run"
    const val COMMAND_TEST = "test"
  }
}
