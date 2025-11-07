package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

class PreferClassJarsOverSourcelessJarsSection : BooleanScalarSection() {
  override val name: String = NAME
  override val sectionKey: SectionKey<Boolean> = KEY
  override val doc: String = "In case library class jar is also present in the sources jars list, " +
    "prefer the class jar over the source jar."
  override val default: Boolean = true

  companion object {
    const val NAME = "prefer_class_jars_over_sourceless_jars"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
