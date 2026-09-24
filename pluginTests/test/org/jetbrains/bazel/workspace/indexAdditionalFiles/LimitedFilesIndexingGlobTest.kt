package org.jetbrains.bazel.workspace.indexAdditionalFiles

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.languages.projectview.INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.INDEX_ALL_FILES_IN_DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.workspace.model.test.framework.WorkspaceModelBaseTest
import org.junit.jupiter.api.Test
import java.nio.file.Path

class LimitedFilesIndexingGlobTest : WorkspaceModelBaseTest() {

  @Test
  fun `should match default Bazel additional files`() {
    val glob = limitedGlob()

    assertThat(glob.matches(projectPath("foo.bzl"))).isTrue()
    assertThat(glob.matches(projectPath("BUILD"))).isTrue()
    assertThat(glob.matches(projectPath("BUILD.bazel"))).isTrue()
    assertThat(glob.matches(projectPath("WORKSPACE"))).isTrue()
    assertThat(glob.matches(projectPath("MODULE.bazel"))).isTrue()
  }

  @Test
  fun `should not match unrelated files`() {
    val glob = limitedGlob()

    assertThat(glob.matches(projectPath("foo.txt"))).isFalse()
    assertThat(glob.matches(projectPath("foo.md"))).isFalse()
    assertThat(glob.matches(projectPath("foo.unknown"))).isFalse()
  }

  @Test
  fun `should match custom project view additional file patterns`() {
    val projectView = projectView(
      INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY to listOf("*.custom"),
    )

    val glob = limitedGlob(projectView)

    assertThat(glob.matches(projectPath("foo.custom"))).isTrue()
    assertThat(glob.matches(projectPath("nested/foo.custom"))).isTrue()
    assertThat(glob.matches(projectPath("foo.txt"))).isFalse()
  }

  @Test
  fun `should not create limited glob when all files in directories are indexed`() {
    val projectView = projectView(
      INDEX_ALL_FILES_IN_DIRECTORIES_KEY to true,
    )

    val glob = project.limitedFilesIndexingGlobOrNull(projectView)

    assertThat(glob).isNull()
  }

  private fun limitedGlob(projectView: ProjectView = ProjectView.EMPTY): ProjectViewGlobSet =
    requireNotNull(project.limitedFilesIndexingGlobOrNull(projectView))

  private fun projectPath(relativePath: String): Path = projectBasePath.resolve(relativePath)

  private fun projectView(vararg sections: Pair<SectionKey<*>, Any>): ProjectView = ProjectView(sections.toMap(), emptyList())
}
