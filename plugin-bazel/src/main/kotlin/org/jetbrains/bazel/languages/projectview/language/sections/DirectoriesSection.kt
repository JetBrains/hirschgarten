package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.completion.DirectoriesCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ExcludableValue
import org.jetbrains.bazel.languages.projectview.language.ListSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey
import java.nio.file.Path

class DirectoriesSection : ListSection<List<ExcludableValue<Path>>>() {
  override val name = NAME
  override val default = emptyList<ExcludableValue<Path>>()
  override val sectionKey = KEY
  override val completionProvider = DirectoriesCompletionProvider()
  override val doc =
    "A list of directories to include in your project. All files in the given " +
      "directories will be indexed (allowing you to search for them) and listed in the " +
      "Project tool window. If a file is not included in your project, it will have a " +
      "yellow tab, and you will see a warning when attempting to edit it."

  override fun fromRawValues(rawValues: List<String>): List<ExcludableValue<Path>> = rawValues.mapNotNull(::parseItem)

  companion object {
    const val NAME = "directories"
    val KEY = SectionKey<List<ExcludableValue<Path>>>(NAME)

    private fun pathOfOrNull(path: String): Path? =
      try {
        Path.of(path)
      } catch (ex: Exception) {
        null
      }

    private fun parseItem(item: String): ExcludableValue<Path>? {
      return if (item.startsWith("-")) {
        val path = pathOfOrNull(item.substring(1)) ?: return null
        ExcludableValue.excluded(path)
      } else {
        val path = pathOfOrNull(item) ?: return null
        ExcludableValue.included(path)
      }
    }
  }
}
