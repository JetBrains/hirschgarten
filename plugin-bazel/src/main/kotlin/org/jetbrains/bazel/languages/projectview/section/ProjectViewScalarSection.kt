package org.jetbrains.bazel.languages.projectview.section

data class ProjectViewScalarSection(val name: String, val isFollowedByColon: Boolean) {
  companion object {
    // TODO: Add the sections
    val KEYWORD_MAP = emptyMap<String, ProjectViewScalarSection>()
  }
}
