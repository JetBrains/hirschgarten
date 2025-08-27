package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.BooleanScalarSection

class DeriveTargetsFromDirectoriesSection : BooleanScalarSection() {
  override val name = NAME
  override val default = false
  override val sectionKey = KEY
  override val doc =
    "If set to true, relevant project targets will be automatically derived from the " +
      "directories during sync. Note that directories from all transitive imports are " +
      "included."

  companion object {
    const val NAME = "derive_targets_from_directories"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
