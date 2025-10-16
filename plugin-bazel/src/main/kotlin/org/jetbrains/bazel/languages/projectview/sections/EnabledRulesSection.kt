package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.ListSection
import org.jetbrains.bazel.languages.projectview.SectionKey

class EnabledRulesSection : ListSection<List<String>>() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "A list of enabled Bazel rules."

  override fun fromRawValues(rawValues: List<String>): List<String> = rawValues

  companion object {
    const val NAME = "enabled_rules"
    val KEY = SectionKey<List<String>>(NAME)
  }
}
