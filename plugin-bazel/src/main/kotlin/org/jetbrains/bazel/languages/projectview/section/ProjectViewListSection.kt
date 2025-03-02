package org.jetbrains.bazel.languages.projectview.section

data class ProjectViewListSection(val name: String) {
  companion object {
    // TODO: Add the sections
    val KEYWORD_MAP = emptyMap<String, ProjectViewListSection>()
  }
}
