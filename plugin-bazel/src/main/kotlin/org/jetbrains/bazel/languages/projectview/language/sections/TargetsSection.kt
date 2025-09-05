package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ExcludableValue
import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.ExcludableListSection

class TargetsSection : ExcludableListSection<Label>() {
  override val name = NAME
  override val sectionKey = KEY
  override val default = emptyList<ExcludableValue<Label>>()
  override val completionProvider = TargetCompletionProvider()
  override val doc =
    "A list of bazel target expressions. To resolve source files under an imported directory, the source must be " +
      "reachable from one of your targets. Because these are full bazel target expressions, they support /... notation."

  override fun parseItem(value: String): Label = Label.parse(value)

  companion object {
    const val NAME = "targets"
    val KEY = SectionKey<List<ExcludableValue<Label>>>(NAME)
  }
}
