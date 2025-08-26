package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.language.SectionKey
import org.jetbrains.bazel.languages.projectview.language.sections.presets.BooleanScalarSection

class IndexAllFilesInDirectoriesSection : BooleanScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Whether to all index files inside [ProjectViewDirectoriesSection] or just sources of targets"

  companion object {
    const val NAME = "index_all_files_in_directories"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
