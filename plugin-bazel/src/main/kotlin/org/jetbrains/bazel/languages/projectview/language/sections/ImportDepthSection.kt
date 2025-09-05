package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.IntScalarSection

class ImportDepthSection : IntScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Specifies the maximum depth of subdirectories to be imported."

  companion object {
    const val NAME = "import_depth"
    val KEY = SectionKey<Int>(NAME)
  }
}
