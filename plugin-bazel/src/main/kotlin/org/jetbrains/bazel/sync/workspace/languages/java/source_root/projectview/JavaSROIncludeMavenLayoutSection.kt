package org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

class JavaSROIncludeMavenLayoutSection : BooleanScalarSection() {
  override val name: String = NAME
  override val sectionKey: SectionKey<Boolean> = KEY
  override val default: Boolean? = true

  companion object {
    const val NAME = "java_sro_include_maven_layout"
    val KEY: SectionKey<Boolean> = SectionKey(NAME)
  }
}
