package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.ListSection
import org.jetbrains.bazel.languages.projectview.SectionKey
import java.nio.file.Path

@ApiStatus.Internal
class NonBazelPythonDirectoriesSection : ListSection<List<Path>>() {
  override val name = NAME
  override val default = emptyList<Path>()
  override val sectionKey = KEY
  override val doc =
    "A list of directories containing Python files that are not part of any Bazel target. " +
      "The plugin will create one IntelliJ module per listed directory and assign a Python SDK to it, " +
      "ensuring those files have code intelligence without being part of a Bazel python_library or py_binary target. " +
      "Paths are relative to the workspace root."

  override fun fromRawValues(rawValues: List<String>): List<Path> =
    rawValues.mapNotNull { value ->
      try {
        Path.of(value)
      } catch (_: Exception) {
        null
      }
    }

  companion object {
    const val NAME = "non_bazel_python_directories"
    val KEY = SectionKey<List<Path>>(NAME)
  }
}
