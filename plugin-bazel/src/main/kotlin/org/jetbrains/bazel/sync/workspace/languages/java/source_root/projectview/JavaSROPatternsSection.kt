package org.jetbrains.bazel.sync.workspace.languages.java.source_root.projectview

import org.jetbrains.bazel.languages.projectview.ListSection
import org.jetbrains.bazel.languages.projectview.SectionKey

class JavaSROPatternsSection : ListSection<List<String>>() {
  override val name: String = NAME
  override val sectionKey: SectionKey<List<String>> = KEY
  override val doc: String = "Patterns for java source root optimization"
  override val default: List<String> = listOf(
    "src/main/java",
    "src/test/java",
    "src/main/kotlin",
    "src/test/kotlin",
    "src/java",
    "src/kotlin",
  )

  override fun fromRawValues(rawValues: List<String>): List<String> = rawValues

  companion object {
    const val NAME = "java_source_root_optimization_patterns"
    val KEY: SectionKey<List<String>> = SectionKey(NAME)
  }
}
