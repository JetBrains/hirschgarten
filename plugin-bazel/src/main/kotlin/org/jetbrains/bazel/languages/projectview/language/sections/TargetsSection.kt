package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ExcludableValue
import org.jetbrains.bazel.languages.projectview.language.ListSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class TargetsSection : ListSection<List<ExcludableValue<Label>>>() {
  override val name = NAME

  override val default = emptyList<ExcludableValue<Label>>()

  override val doc =
    "A list of bazel target expressions. To resolve source files under an imported directory, the source must be " +
      "reachable from one of your targets. Because these are full bazel target expressions, they support /... notation."

  override val completionProvider = TargetCompletionProvider()

  override val sectionKey = KEY

  private fun parseItem(item: String): ExcludableValue<Label> {
    if (item.startsWith("-")) {
      val labelStr = item.substring(1)
      return Label.parse(labelStr).let { ExcludableValue.excluded(it) }
    } else {
      return Label.parse(item).let { ExcludableValue.included(it) }
    }
  }

  override fun fromRawValues(rawValues: List<String>): List<ExcludableValue<Label>> = rawValues.map(::parseItem)

  companion object {
    const val NAME = "targets"
    val KEY = SectionKey<List<ExcludableValue<Label>>>(NAME)
  }
}
