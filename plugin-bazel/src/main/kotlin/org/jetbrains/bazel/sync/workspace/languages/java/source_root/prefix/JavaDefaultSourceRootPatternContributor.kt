package org.jetbrains.bazel.sync.workspace.languages.java.source_root.prefix

import com.intellij.openapi.project.Project

private class JavaDefaultSourceRootPatternContributor : JavaSourceRootPatternContributor {
  private val prefixes = listOf(
    "src/main/java",
    "src/test/java",
    "src/main/kotlin",
    "src/test/kotlin",
    "src/java",
    "src/kotlin",
  )

  override fun getIncludePatterns(project: Project): List<SourceRootPattern> =
    listOf { path: String -> prefixes.any { path.contains(it) } }

  override fun getExcludePatterns(project: Project): List<SourceRootPattern> = listOf()
}
