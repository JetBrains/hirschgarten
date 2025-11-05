package org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

class JavaSROEnableSection : BooleanScalarSection() {
  override val name: String = NAME
  override val sectionKey: SectionKey<Boolean> = KEY
  override val doc: String = "Enable java source root optimization"

  override val default: Boolean = false

  companion object {
    const val NAME = "java_sro_enable"
    val KEY: SectionKey<Boolean> = SectionKey(NAME)
  }
}
