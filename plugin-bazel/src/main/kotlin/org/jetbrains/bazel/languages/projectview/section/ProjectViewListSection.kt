package org.jetbrains.bazel.languages.projectview.section

data class ProjectViewListSection(val name: String) {
  companion object {
    val KEYWORD_MAP = emptyMap<String, ProjectViewListSection>()
  }
}
