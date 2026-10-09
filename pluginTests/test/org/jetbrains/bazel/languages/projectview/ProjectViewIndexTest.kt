package org.jetbrains.bazel.languages.projectview

import io.kotest.matchers.collections.shouldContainExactly
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.junit.jupiter.api.Test

@BazelTestApplication
class ProjectViewIndexTest {

  @Test
  fun `should use the index section when it is set`() {
    val projectView = projectView(
      INDEX_KEY to listOf("*.xml"),
      INDEX_ALL_FILES_IN_DIRECTORIES_KEY to true,
    )

    projectView.index.shouldContainExactly("*.xml")
  }

  @Test
  fun `should migrate index_all_files_in_directories to a match-all pattern`() {
    val projectView = projectView(INDEX_ALL_FILES_IN_DIRECTORIES_KEY to true)

    projectView.index.shouldContainExactly("*")
  }

  @Test
  fun `should migrate index_additional_files_in_directories patterns`() {
    val projectView = projectView(INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY to listOf("*.custom"))

    projectView.index.shouldContainExactly("*.custom")
  }

  private fun projectView(vararg sections: Pair<ProjectViewSectionKey<*>, Any>): ProjectView = ProjectView(sections.toMap(), emptyList())
}
