package org.jetbrains.bazel.languages.projectview.language

sealed interface ProjectViewSection {
  interface Number : ProjectViewSection

  interface Boolean : ProjectViewSection

  interface List<T> : ProjectViewSection

  companion object {
    val KEYWORD_MAP = emptyMap<ProjectViewSyntaxKey, ProjectViewSection>()
  }
}
