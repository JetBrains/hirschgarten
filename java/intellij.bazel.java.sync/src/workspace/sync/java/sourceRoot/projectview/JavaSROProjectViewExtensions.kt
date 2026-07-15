package org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.projectview

import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.SectionKey

internal val JAVA_SOURCE_ROOT_OPTIMIZATION_KEY: SectionKey<Boolean> = SectionKey("java_source_root_optimization_enable", false)
internal val ProjectView.javaSROEnable: Boolean
  get() = getSection(JAVA_SOURCE_ROOT_OPTIMIZATION_KEY)

internal val JAVA_SOURCE_ROOT_OPTIMIZATION_PATTERNS_KEY: SectionKey<List<String>> = SectionKey(
  "java_source_root_optimization_patterns",
  listOf(
    "src/main/java",
    "src/test/java",
    "src/main/kotlin",
    "src/test/kotlin",
    "src/java",
    "src/kotlin",
  ),
)
internal val ProjectView.javaSROPatterns: List<String>
  get() = getSection(JAVA_SOURCE_ROOT_OPTIMIZATION_PATTERNS_KEY)
