package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.ListSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class PythonCodeGeneratorRuleNamesSection : ListSection<List<String>>() {
  override val name = NAME
  override val sectionKey = KEY

  override fun fromRawValues(rawValues: List<String>): List<String> = rawValues

  companion object {
    const val NAME = "python_code_generator_rule_names"
    val KEY = SectionKey<List<String>>(NAME)
  }
}
