package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.BooleanScalarSection

class ImportIjarsSection : BooleanScalarSection() {
  override val name = NAME
  override val sectionKey = KEY

  companion object {
    const val NAME = "import_ijars"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
