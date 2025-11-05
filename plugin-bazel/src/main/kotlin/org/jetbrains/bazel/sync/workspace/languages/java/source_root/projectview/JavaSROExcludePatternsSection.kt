package org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview

import org.jetbrains.bazel.languages.projectview.ListSection
import org.jetbrains.bazel.languages.projectview.SectionKey

class JavaSROExcludePatternsSection : ListSection<List<String>>() {
  override val name: String = NAME
  override val sectionKey: SectionKey<List<String>> = KEY
  override val doc: String = "Exclude patterns for java source root optimization"

  override fun fromRawValues(rawValues: List<String>): List<String> = rawValues

  companion object {
    const val NAME = "java_sro_exclude_patterns"
    val KEY: SectionKey<List<String>> = SectionKey(NAME)
  }
}
