package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.BooleanScalarSection

class DeriveInstrumentationFilterFromTargetsSection : BooleanScalarSection() {
  override val name = NAME
  override val sectionKey = KEY

  companion object {
    const val NAME = "derive_instrumentation_filter_from_targets"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
