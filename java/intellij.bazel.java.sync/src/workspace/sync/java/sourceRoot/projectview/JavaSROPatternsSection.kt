package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview

import org.jetbrains.bazel.languages.projectview.ListSection
import org.jetbrains.bazel.languages.projectview.SectionKey

internal class JavaSROPatternsSection : ListSection<List<String>>() {
  override val sectionKey: SectionKey<List<String>> = JAVA_SOURCE_ROOT_OPTIMIZATION_PATTERNS_KEY
  override val doc: String = "Patterns for java source root optimization"

  override fun fromRawValues(rawValues: List<String>): List<String> = rawValues
}
