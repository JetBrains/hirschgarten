package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.completion.DirectoriesCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ExcludableValue
import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.ExcludableListSection
import java.nio.file.Path

class DirectoriesSection : ExcludableListSection<Path>() {
  override val name = NAME
  override val default = emptyList<ExcludableValue<Path>>()
  override val sectionKey = KEY
  override val completionProvider = DirectoriesCompletionProvider()
  override val doc =
    "A list of directories to include in your project. All files in the given " +
      "directories will be indexed (allowing you to search for them) and listed in the " +
      "Project tool window. If a file is not included in your project, it will have a " +
      "yellow tab, and you will see a warning when attempting to edit it."

  override fun parseItem(value: String): Path? {
    return try {
      Path.of(value)
    } catch (_: Exception) {
      return null
    }
  }

  companion object {
    const val NAME = "directories"
    val KEY = SectionKey<List<ExcludableValue<Path>>>(NAME)
  }
}
