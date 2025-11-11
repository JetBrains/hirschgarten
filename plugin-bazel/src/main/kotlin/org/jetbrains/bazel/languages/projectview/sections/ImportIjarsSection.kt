package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

class ImportIjarsSection : BooleanScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc: String =
    "Specifies whether to prefer interface/header jars (ijars) for JVM libraries instead of full jars during import. " +
      "Interface/header jars contain only public API class stubs and not the full bytecode. " +
      "This can reduce the size of the index and improve import performance at the cost of " +
      "navigation to library implementations and false-positives in some of the IDE warnings. The default is false."

  companion object {
    const val NAME = "import_ijars"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
