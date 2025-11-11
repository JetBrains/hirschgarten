package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.IntScalarSection

class ImportDepthSection : IntScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc =
    "Specifies how many levels of Bazel targets' dependencies should be imported as modules. " +
      "Only the targets that are present in the workspace are imported. You can use a negative value to import all transitive dependencies. " +
      "The default value is -1, meaning that all transitive context is included."

  companion object {
    const val NAME = "import_depth"
    val KEY = SectionKey<Int>(NAME)
  }
}
