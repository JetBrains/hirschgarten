package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

class DeriveInstrumentationFilterFromTargetsSection : BooleanScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val default: Boolean = true

  companion object {
    const val NAME = "derive_instrumentation_filter_from_targets"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
